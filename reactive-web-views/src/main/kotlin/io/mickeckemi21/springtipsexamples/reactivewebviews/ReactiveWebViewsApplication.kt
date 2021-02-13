package io.mickeckemi21.springtipsexamples.reactivewebviews

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream
import kotlin.properties.Delegates

@SpringBootApplication
class ReactiveWebViewsApplication

fun main(args: Array<String>) {
	runApplication<ReactiveWebViewsApplication>(*args)
}

@Controller
class ReactiveThymeleafGreetingController(private val greetingProducer: GreetingProducer) {

	@GetMapping("/greeting.do")
	fun greetingView(): String = "greeting"

	@GetMapping("/greeting-update.do", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun greetingUpdate(@RequestParam("name") name: String, model: Model): String {
		val greetingsVariable = ReactiveDataDriverContextVariable(greetingProducer.greet(name), 1)
		model.addAttribute("greetings", greetingsVariable)
		return "greeting :: #greeting-block"
	}

}

@Configuration
class WebSocketConfig {

	@Bean
	fun urlHandlerMapping(gwh: WebSocketHandler): SimpleUrlHandlerMapping =
		SimpleUrlHandlerMapping().apply {
			order = 10
			urlMap = mapOf("ws/greeting" to gwh)
		}

	@Bean
	fun webHandlerAdapter(): WebSocketHandlerAdapter =
		WebSocketHandlerAdapter()

	@Bean
	fun gwh(greetingProducer: GreetingProducer): WebSocketHandler = WebSocketHandler { session ->
		val greetings = session.receive()
			.map(WebSocketMessage::getPayloadAsText)
			.flatMap(greetingProducer::greet)
			.map(Greeting::message)
			.map(session::textMessage)

		session.send(greetings)
	}

}

@RestController
class SseGreetingController(private val greetingProducer: GreetingProducer) {

	@GetMapping(value = ["/sse/greeting/{name}"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
	fun greet(@PathVariable name: String): Flux<String> =
		greetingProducer.greet(name).map(Greeting::message)

}

@Component
class GreetingProducer {

	fun greet(name: String): Flux<Greeting> =
		Flux.fromStream(Stream.generate { Greeting("Hello $name @ ${Instant.now().toString()}!") })
			.delayElements(Duration.ofSeconds(1L))

}

class Greeting() {
	var message by Delegates.notNull<String>()

	constructor(message: String) : this() {
		this.message = message
	}

}