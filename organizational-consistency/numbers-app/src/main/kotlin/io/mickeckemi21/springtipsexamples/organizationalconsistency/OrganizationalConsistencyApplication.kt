package io.mickeckemi21.springtipsexamples.organizationalconsistency

import io.mickeckemi21.springtipsexamples.organizationalconsistency.numbers.NumberService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@SpringBootApplication
class OrganizationalConsistencyApplication

fun main(args: Array<String>) {
    runApplication<OrganizationalConsistencyApplication>(*args)
}

@Component
class App(private val numberService: NumberService) {

    companion object {
        private val log = LoggerFactory.getLogger(App::class.java)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReadyEvent() {
        // @formatter:off
        log.info("new random number: ${numberService.generateRandomNumber()}") //
        // @formatter:on
    }

}
