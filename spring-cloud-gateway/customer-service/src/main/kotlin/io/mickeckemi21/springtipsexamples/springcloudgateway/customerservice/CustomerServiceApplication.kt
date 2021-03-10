package io.mickeckemi21.springtipsexamples.springcloudgateway.customerservice

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration.ofSeconds

@SpringBootApplication
class CustomerServiceApplication {

    companion object {
        private val log = LoggerFactory.getLogger(CustomerServiceApplication::class.java)
    }

    @Bean
    fun appRunner(
        customerRepository: CustomerRepository
    ): ApplicationRunner = ApplicationRunner {
        customerRepository.deleteAll()
            .thenMany(Flux.just("A", "B", "C")
                .map { Customer(it) }
                .flatMap(customerRepository::save))
            .thenMany(customerRepository.findAll())
            .subscribe { log.info(it.toString()) }
    }

    @Bean
    fun routes(
        customerRepository: CustomerRepository
    ): RouterFunction<ServerResponse> = router {
        GET("/customers") {
            ServerResponse.ok()
                .body(
                    customerRepository.findAll(),
                    Customer::class.java
                )
        }
        GET("/customers/{id}") {
            ServerResponse.ok()
                .body(
                    customerRepository.findById(it.pathVariable("id")),
                    Customer::class.java
                )
        }
        GET("/delay") {
            ServerResponse.ok()
                .body(
                    Mono.just("Hello!").delayElement(ofSeconds(10L)),
                    String::class.java
                )
        }
    }

}

fun main(args: Array<String>) {
    runApplication<CustomerServiceApplication>(*args)
}

interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
data class Customer(
    val name: String,
    @Id
    val id: String? = null
)
