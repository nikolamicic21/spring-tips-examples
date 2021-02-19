package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.util.function.Predicate

@DataMongoTest
internal class ReservationDocumentTest {

    @Autowired
    private lateinit var reactiveMongoTemplate: ReactiveMongoTemplate

    @Test
    fun persist() {
        val one = Reservation(null, "One")
        val two = Reservation(null, "Two")

        val saved = Flux.just(one, two)
            .flatMap { reactiveMongoTemplate.save(it) }

        val predicate = Predicate<Reservation> {
            StringUtils.hasText(it.id) && StringUtils.hasText(it.name)
        }

        StepVerifier.create(saved)
            .expectNextMatches(predicate)
            .expectNextMatches(predicate)
            .verifyComplete()
    }
}
