package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ReservationTest {

    @Test
    fun create() {
        val reservation = Reservation(null, "Bob")

        Assertions.assertNull(reservation.id)
        Assertions.assertEquals("Bob", reservation.name)
    }

}