package io.mickeckemi21.springtipsexamples.bootifultesting

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ReservationRestController(private val reservationRepository: ReservationRepository) {

    @GetMapping("/reservations")
    fun reservations(): Collection<Reservation> = reservationRepository.findAll()

}