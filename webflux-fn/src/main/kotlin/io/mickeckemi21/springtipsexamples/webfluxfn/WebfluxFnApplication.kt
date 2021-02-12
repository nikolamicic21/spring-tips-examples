package io.mickeckemi21.springtipsexamples.webfluxfn

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters.fromPublisher
import org.springframework.web.reactive.function.server.RequestPredicates.GET
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.stream.Stream

fun main(args: Array<String>) {
    runApplication<WebfluxFnApplication>(*args)
}

@SpringBootApplication
class WebfluxFnApplication {

    @Bean
    fun router(personRouteHandler: PersonRouteHandler): RouterFunction<ServerResponse> =
        route(GET("/persons"), personRouteHandler::all)
            .and(route(GET("/persons/{id}"), personRouteHandler::byId))

}

@Component
class PersonRouteHandler(private val personRepository: PersonRepository) {

    fun all(request: ServerRequest): Mono<ServerResponse> {
        val fromStream = Flux.fromStream(personRepository.all())
        return ServerResponse.ok()
            .body(fromPublisher(fromStream, Person::class.java))
    }

    fun byId(request: ServerRequest): Mono<ServerResponse> =
        personRepository.findById(request.pathVariable("id"))
            .map { Mono.just(it) }
            .map { ServerResponse.ok().body(fromPublisher(it, Person::class.java)) }
            .orElseThrow { IllegalArgumentException("Oops!") }

}

@Component
class SampleDataClr(private val personRepository: PersonRepository) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(SampleDataClr::class.java)
    }

    override fun run(vararg args: String?) {
        Stream.of("Person1", "Person2", "Person3", "Person4")
            .forEach {
                personRepository.save(Person().apply {
                    name = it
                    age = (18..100).random()
                })
            }
        personRepository.findAll().forEach { log.info(it.toString()) }
    }

}

interface PersonRepository : MongoRepository<Person, String> {

    @Query("{}")
    fun all(): Stream<Person>

}

@Document
class Person {
    @Id
    var id: String? = null
    var name: String? = null
    var age: Int? = null

    override fun toString(): String {
        return "Person(id=$id, name=$name, age=$age)"
    }
}
