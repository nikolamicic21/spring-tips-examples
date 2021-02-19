package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.consumer

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

@Component
class ReservationClient(private val client: WebClient) {

    fun getAllReservation(): Flux<Reservation> =
        client.get()
            .uri("http://localhost:8080/reservations")
            .exchangeToFlux { it.bodyToFlux(Reservation::class.java) }

}