package io.mickeckemi21.rsocketspringboot.consumer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

@SpringBootApplication
class ProducerApplication

fun main(args: Array<String>) {
    runApplication<ProducerApplication>(*args)
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

@Controller
class GreetingsRSocketController {

    @MessageExceptionHandler
    fun errorHandler(e: IllegalArgumentException): Flux<GreetingsResponse> = Flux
        .just(GreetingsResponse().withGreeting("OOH NOO!"))

    @MessageMapping("error")
    fun error(request: GreetingsRequest): Flux<GreetingsResponse> =
        Flux.error(IllegalArgumentException())

    @MessageMapping("greet")
    fun greet(request: GreetingsRequest): GreetingsResponse =
        GreetingsResponse(request.name!!)

    @MessageMapping("greet.stream")
    fun greetStream(request: GreetingsRequest): Flux<GreetingsResponse> = Flux
        .fromStream(Stream.generate {
            GreetingsResponse(request.name!!)
        }).delayElements(Duration.ofSeconds(1L))


}
