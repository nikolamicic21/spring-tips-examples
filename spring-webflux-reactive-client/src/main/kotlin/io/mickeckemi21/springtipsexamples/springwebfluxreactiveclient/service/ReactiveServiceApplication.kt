package io.mickeckemi21.springtipsexamples.springwebfluxreactiveclient.service

import io.mickeckemi21.springtipsexamples.springwebfluxreactiveclient.Event
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*
import java.util.stream.Stream

@SpringBootApplication
@RestController
class ReactiveServiceApplication {

    @GetMapping("/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun events(): Flux<Event> =
        Flux.fromStream(Stream.generate { Event(System.currentTimeMillis(), Date()) })
            .delayElements(Duration.ofSeconds(1L))

    @GetMapping("/events/{id}")
    fun eventById(@PathVariable id: Long) =
        Mono.just(Event(id, Date()))

}

fun main(args: Array<String>) {
    runApplication<ReactiveServiceApplication>(*args)
}
