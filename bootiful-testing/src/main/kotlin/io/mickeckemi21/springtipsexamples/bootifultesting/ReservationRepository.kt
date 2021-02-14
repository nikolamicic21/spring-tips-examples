package io.mickeckemi21.springtipsexamples.bootifultesting

import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByName(name: String): Collection<Reservation>
}
