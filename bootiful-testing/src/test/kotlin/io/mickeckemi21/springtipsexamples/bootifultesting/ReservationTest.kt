package io.mickeckemi21.springtipsexamples.bootifultesting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ReservationTest {

    @Test
    fun creation() {
        val reservation = Reservation(1L, "Jane")
        Assertions.assertEquals(1L, reservation.id)
        Assertions.assertEquals("Jane", reservation.name)
    }

}