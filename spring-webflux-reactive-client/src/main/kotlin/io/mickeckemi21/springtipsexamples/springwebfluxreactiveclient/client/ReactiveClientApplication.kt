package io.mickeckemi21.springtipsexamples.springwebfluxreactiveclient.client

import io.mickeckemi21.springtipsexamples.springwebfluxreactiveclient.Event
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
class ReactiveClientApplication {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ReactiveClientApplication::class.java)
    }

    @Bean
    fun client() = WebClient.create("http://localhost:8080")

    @Bean
    fun commandLineRunner(client: WebClient) = CommandLineRunner {
        client.get()
            .uri("/events")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .exchangeToFlux { it.bodyToFlux(Event::class.java) }
            .subscribe { event -> log.info(event.toString()) }
    }

}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ReactiveClientApplication::class.java)
        .properties(mapOf("server.port" to "8081"))
        .run(*args)
}
