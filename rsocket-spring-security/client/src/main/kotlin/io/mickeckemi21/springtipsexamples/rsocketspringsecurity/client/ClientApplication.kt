package io.mickeckemi21.springtipsexamples.rsocketspringsecurity.client

import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.connectTcpAndAwait
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Mono
import java.util.*

@SpringBootApplication
class ClientApplication {

	companion object {
		private val log = LoggerFactory.getLogger(ClientApplication::class.java)
	}

	@Bean
	fun rSocketStrategiesCustomizer(): RSocketStrategiesCustomizer = RSocketStrategiesCustomizer {
		it.encoder(SimpleAuthenticationEncoder())
	}

	@Bean
	fun rSocketRequester(builder: RSocketRequester.Builder) = runBlocking {
		builder
			.connectTcpAndAwait("localhost", 7000)
	}


	@Bean
	fun readyEvent(requester: RSocketRequester): ApplicationListener<ApplicationReadyEvent> = ApplicationListener {
		requester
			.route("greetings")
			.metadata(credentials, mimeType)
			.data(Mono.empty<Void>())
			.retrieveFlux(GreetingResponse::class.java)
			.subscribe { log.info("secured response: ${it.message}") }
	}

}

fun main(args: Array<String>) {
	runApplication<ClientApplication>(*args)
	readLine()
}

class GreetingResponse() {
	var message: String? = null

	constructor(message: String) : this() {
		this.message = message
	}
}

val mimeType = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)
val credentials = UsernamePasswordMetadata("user", "pass")