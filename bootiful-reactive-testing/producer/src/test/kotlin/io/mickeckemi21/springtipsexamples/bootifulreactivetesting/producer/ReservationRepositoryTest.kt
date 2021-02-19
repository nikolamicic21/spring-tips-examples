package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@DataMongoTest
internal class ReservationRepositoryTest {

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Test
    fun findByName() {
        val saved = reservationRepository.deleteAll()
            .thenMany(
                Flux.just(
                    Reservation(null, "Jane"),
                    Reservation(null, "Joe"),
                    Reservation(null, "Janet")
                )
                    .flatMap { reservationRepository.save(it) }
            )
            .thenMany(reservationRepository.findByName("Joe"))

        StepVerifier.create(saved)
            .expectNextMatches { reservation -> reservation.name == "Joe" && reservation.id != null }
            .verifyComplete()
    }

}