package io.mickeckemi21.springtipsexamples.springcloudgatewayredux.customerservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

val NAMES = listOf("Jean", "Yuxin", "Mario", "Zhen", "Mia", "Dave")
val COUNTER = AtomicInteger()
val CUSTOMERS = Flux.fromStream(Stream.generate {
    val id = COUNTER.incrementAndGet()
    Customer(id, NAMES[id % NAMES.size])
}).delayElements(Duration.ofSeconds(3L))

@SpringBootApplication
class CustomerServiceApplication

fun main(args: Array<String>) {
    runApplication<CustomerServiceApplication>(*args)
}

@Configuration
class CustomersWebSocketConfiguration(
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun customers(): Flux<Customer> = CUSTOMERS.publish().autoConnect()

    @Bean
    fun customersWebSocketHandler(
        customers: Flux<Customer>
    ): WebSocketHandler = WebSocketHandler { webSocketSession ->
        webSocketSession.send(
            customers
                .map { from(it) }
                .map { webSocketSession.textMessage(it) }
        )
    }

    @Bean
    fun webSocketUrlHandlerMapping(
        customersWebSocketHandler: WebSocketHandler
    ): SimpleUrlHandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws/customers" to customersWebSocketHandler), 10)

    private fun from(customer: Customer): String =
        objectMapper.writeValueAsString(customer)

}

@RestController
class CustomersRestController(
    private val customers: Flux<Customer>
) {

    @GetMapping(value = ["/customers"], produces = [TEXT_EVENT_STREAM_VALUE])
    fun getCustomers(): Flux<Customer> = customers

}

@RestController
class ReliabilityRestController {

    companion object {
        private val log = LoggerFactory.getLogger(ReliabilityRestController::class.java)
    }

    private val errorCount = ConcurrentHashMap<String, AtomicInteger>()
    private val countPerSecond = ConcurrentHashMap<Long, AtomicInteger>()

    @GetMapping("/error/{id}")
    fun errors(@PathVariable id: String): ResponseEntity<Any> {
        errorCount.compute(id) { _, value ->
            if (value == null) {
                val newValue = AtomicInteger(0)
                newValue.incrementAndGet()
                newValue
            } else {
                value.incrementAndGet()
                value
            }
        }

        val count = errorCount[id]!!.get()
        if (count < 5) {
            log.info("returning an error for request #$count for ID '$id'")
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        } else {
            log.info("returning a proper response for request #$count for ID '$id'")
        }

        return ResponseEntity.ok(mapOf("greeting" to "Congrats, $id! You did it!"))
    }

    @GetMapping("/hello")
    fun hello(): String {
        val now = System.currentTimeMillis()
        val second = now / 1000
        val countForTheCurrentSecond = countPerSecond.compute(second) { _, countPerSecond ->
            if (countPerSecond == null) {
                val newCountPerSecond = AtomicInteger(0)
                newCountPerSecond.incrementAndGet()
                newCountPerSecond
            } else {
                countPerSecond.incrementAndGet()
                countPerSecond
            }
        }
        log.info("there have been ${countForTheCurrentSecond!!.get()} requests for the second $second")
        return "Hello World!"
    }

}

data class Customer(
    val id: Int,
    val name: String
)
