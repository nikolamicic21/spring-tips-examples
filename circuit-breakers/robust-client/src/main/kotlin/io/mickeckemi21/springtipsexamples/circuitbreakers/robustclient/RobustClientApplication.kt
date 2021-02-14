package io.mickeckemi21.springtipsexamples.circuitbreakers

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker
import org.springframework.cloud.netflix.hystrix.EnableHystrix
import org.springframework.context.annotation.Primary
import org.springframework.retry.annotation.CircuitBreaker
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    runApplication<RobustClientApplication>(*args)
}

@SpringBootApplication
@EnableCircuitBreaker
@EnableHystrix
@EnableRetry
class RobustClientApplication

@RestController
class ShakyRestController(private val shakyBusinessService: ShakyBusinessService) {

    @GetMapping("/boom")
    fun boom(): Int = this.shakyBusinessService.deriveNumber()

}

interface ShakyBusinessService {
    fun deriveNumber(): Int
}

@Service
@Primary
class HystrixShakyBusinessService : ShakyBusinessService {

    companion object {
        private val log = LoggerFactory.getLogger(HystrixShakyBusinessService::class.java)
    }

    fun fallback(): Int = 2

    @HystrixCommand(fallbackMethod = "fallback")
    override fun deriveNumber(): Int {
        log.info("calling deriveNumber()")
        if (Math.random() > .5) {
            Thread.sleep(1000 * 3)
            throw BoomException("Boom!")
        }
        return 1
    }
}

@Service
class RetryShakyBusinessService : ShakyBusinessService {

    companion object {
        private val log = LoggerFactory.getLogger(RetryShakyBusinessService::class.java)
    }

    @Recover
    fun fallback(e: RuntimeException): Int = 2

    @Retryable(include = [BoomException::class])
    override fun deriveNumber(): Int {
        log.info("calling deriveNumber()")
        if (Math.random() > .5) {
            Thread.sleep(1000 * 3)
            throw BoomException("Boom!")
        }
        return 1
    }
}

@Service
//@Primary
class CircuitBreakerRetryShakyBusinessService : ShakyBusinessService {

    companion object {
        private val log = LoggerFactory.getLogger(RetryShakyBusinessService::class.java)
    }

    @Recover
    fun fallback(e: RuntimeException): Int = 2

    @CircuitBreaker(include = [BoomException::class], openTimeout = 2000L, maxAttempts = 1)
    override fun deriveNumber(): Int {
        log.info("calling deriveNumber()")
        if (Math.random() > .5) {
            Thread.sleep(1000 * 3)
            throw BoomException("Boom!")
        }
        return 1
    }
}

class BoomException(message: String) : RuntimeException(message)
