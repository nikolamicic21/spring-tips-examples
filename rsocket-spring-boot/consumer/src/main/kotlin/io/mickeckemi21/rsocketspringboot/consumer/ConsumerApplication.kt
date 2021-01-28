package io.mickeckemi21.rsocketspringboot.consumer

import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE
import org.springframework.util.MimeTypeUtils.parseMimeType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

@SpringBootApplication
class ConsumerApplication {

    @Bean
    fun rSocketRequester(
        strategies: RSocketStrategies,
        builder: RSocketRequester.Builder
    ): RSocketRequester =
        builder
            .dataMimeType(parseMimeType(APPLICATION_JSON_VALUE))
            .rsocketStrategies(strategies)
            .tcp("localhost", 7000)

}

fun main(args: Array<String>) {
    runApplication<ConsumerApplication>(*args)
}

class GreetingsRequest() {
    var name: String? = null

    constructor(name: String) : this() {
        this.name = name
    }

}

class GreetingsResponse() {
    var greeting: String? = null

    constructor(name: String) : this() {
        withGreeting("Hello $name @ ${Instant.now()}")
    }

    fun withGreeting(msg: String): GreetingsResponse {
        greeting = msg
        return this
    }

}

@RestController
class GreetingsRestController(private val requester: RSocketRequester) {

    @GetMapping("/greet/{name}")
    fun greet(@PathVariable name: String): Publisher<GreetingsResponse> =
        requester
            .route("greet")
            .data(GreetingsRequest(name))
            .retrieveMono(GreetingsResponse::class.java)

    @GetMapping(value = ["/greet/stream/{name}"], produces = [TEXT_EVENT_STREAM_VALUE])
    fun greetSse(@PathVariable name: String): Publisher<GreetingsResponse> =
        requester
            .route("greet.stream")
            .data(GreetingsRequest(name))
            .retrieveFlux(GreetingsResponse::class.java)

    @GetMapping("/error")
    fun error(): Publisher<GreetingsResponse> =
        requester
            .route("error")
            .data(GreetingsRequest())
            .retrieveFlux(GreetingsResponse::class.java)

}
