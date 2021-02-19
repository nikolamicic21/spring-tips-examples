package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ReservationRepository : ReactiveMongoRepository<Reservation, String> {

    fun findByName(name: String): Flux<Reservation>

}
