package io.mickeckemi21.springtipsexamples.springkotlinredux.reactive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.support.beans
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.kotlin.core.publisher.toFlux

@SpringBootApplication
class ReactiveApplication {

    @Bean
    fun routeLocator(
        builder: RouteLocatorBuilder
    ): RouteLocator = builder
        .routes()
        .route("blog") { predicateSpec -> predicateSpec
            .path("/blog").or().path("/atom")
            .filters { gatewayFilterSpec -> gatewayFilterSpec
                .rewritePath("/blog", "/blog.atom")
                .rewritePath("/atom", "/blog.atom")
            }
            .uri("http://spring.io")
        }
        .build()

}

fun main(args: Array<String>) {
    SpringApplicationBuilder()
        .initializers(beans {
            bean {
                val customerRepository = ref<CustomerRepository>()
                val customers = listOf("Jenny", "Marcus", "Stephan")
                    .toFlux()
                    .map { Customer(it) }
                    .flatMap { customerRepository.save(it) }
                customerRepository
                    .deleteAll()
                    .thenMany(customers)
                    .thenMany(customerRepository.findAll())
                    .subscribe { println(it) }
            }
            bean {
                val customerRepository = ref<CustomerRepository>()
                router {
                    GET("/customers") {
                        ServerResponse.ok().body(customerRepository.findAll())
                    }
                    GET("/customers/{id}") {
                        ServerResponse.ok().body(customerRepository.findById(it.pathVariable("id")))
                    }
                }
            }
        })
        .sources(ReactiveApplication::class.java)
        .run(*args)
}

interface CustomerRepository : ReactiveMongoRepository<Customer, String>

@Document
data class Customer(
    val name: String,
    @Id
    var id: String? = null
)
