package io.mickeckemi21.springtipsexamples.bpmflowable

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@SpringBootApplication
class BpmFlowableApplication

fun main(args: Array<String>) {
    runApplication<BpmFlowableApplication>(*args)
}

@Service
class EmailService {

    val sends = ConcurrentHashMap<String, AtomicInteger>()

    companion object {
        private val log = LoggerFactory.getLogger(EmailService::class.java)
    }

    fun sendWelcomeEmail(customerId: String, email: String) {
        // todo
        log.info("sending welcome email for $customerId to $email")
        sends.computeIfAbsent(email) { AtomicInteger() }
        sends[email]!!.incrementAndGet()
    }

}
