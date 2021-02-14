package io.mickeckemi21.springtipsexamples.bootifultesting

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
internal class ReservationRepositoryTest {

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

    @Test
    fun findByReservationName() {
        reservationRepository.save(Reservation(null, "Jane"))
        val byReservationName = reservationRepository.findByName("Jane")
        Assertions.assertEquals(1, byReservationName.size)
        Assertions.assertTrue(byReservationName.iterator().next().id!! > 0L)
        Assertions.assertEquals("Jane", byReservationName.iterator().next().name)
    }

}