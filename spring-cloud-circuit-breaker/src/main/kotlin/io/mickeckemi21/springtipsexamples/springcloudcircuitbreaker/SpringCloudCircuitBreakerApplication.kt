package io.mickeckemi21.springtipsexamples.springcloudcircuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootApplication
class SpringCloudCircuitBreakerApplication {

    @Bean
    fun resilience4JCircuitBreakerFactory(): ReactiveResilience4JCircuitBreakerFactory =
        ReactiveResilience4JCircuitBreakerFactory().apply {
            configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .timeLimiterConfig(
                        TimeLimiterConfig
                            .custom()
                            .timeoutDuration(Duration.ofSeconds(3L))
                            .build()
                    )
                    .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                    .build()
            }
        }

}

fun main(args: Array<String>) {
    runApplication<SpringCloudCircuitBreakerApplication>(*args)
}

@RestController
class FailingGreetingRestController(
    private val failingGreetingService: FailingGreetingService,
    circuitBreakerFactory: ReactiveResilience4JCircuitBreakerFactory
) {

    private val circuitBreaker = circuitBreakerFactory.create("greeting")

    @GetMapping("/greet")
    fun greet(@RequestParam name: String?): Publisher<String> {
        val results = failingGreetingService.greet(name)
        return circuitBreaker.run(results) { Mono.just("Hello world!") }
    }

}

@Service
class FailingGreetingService {

    companion object {
        private val log = LoggerFactory.getLogger(FailingGreetingService::class.java)
    }

    fun greet(name: String?): Mono<String> {
        val seconds = (0..10).random()
        return if (name != null) {
            val msg = "Hello $name! (in ${seconds}s)"
            log.info(msg)
            Mono.just(msg)
                .delayElement(Duration.ofSeconds(seconds.toLong()))
        } else {
            Mono.error(IllegalArgumentException())
        }
    }

}
