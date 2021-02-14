package io.mickeckemi21.springtipsexamples.bootifultesting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager

@DataJpaTest
internal class ReservationJpaTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    fun mapping() {
        val reservation = Reservation(null, "Jane")
        val persistedReservation = testEntityManager.persistFlushFind(reservation)
        Assertions.assertNotNull(persistedReservation.id)
        Assertions.assertTrue(persistedReservation.id!! > 0L)
        Assertions.assertEquals("Jane", persistedReservation.name)
    }

}