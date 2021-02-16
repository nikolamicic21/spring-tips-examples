package io.mickeckemi21.springtipsexamples.springdatar2dbc

import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.DatabasePopulator
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@DataR2dbcTest
internal class PlainR2DbcReservationRepositoryTest {
    
    @Autowired
    private lateinit var reservationRepository: PlainR2DbcReservationRepository

    @TestConfiguration
    class PlainR2DbcReservationRepositoryTestConfig {

        @Bean
        fun reservationRepository(connectionFactory: ConnectionFactory): PlainR2DbcReservationRepository =
            PlainR2DbcReservationRepository(connectionFactory)

        @Bean
        fun connectionFactoryInitializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer = ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(ResourceDatabasePopulator(
                ClassPathResource("schema.sql")
            ))
        }

    }

    @Test
    fun all() {
        val deleteAll = reservationRepository.findAll()
            .flatMap { reservation ->
                reservationRepository.deleteById(reservation.id!!)
            }
        StepVerifier.create(deleteAll)
            .expectNextCount(0L)
            .verifyComplete()

        val saveThreeReservations = Flux.just("First", "Second", "Third")
            .map { name -> Reservation(null, name) }
            .flatMap { reservationRepository.save(it) }

        StepVerifier.create(saveThreeReservations)
            .expectNextCount(3L)
            .verifyComplete()

        val findAll = reservationRepository.findAll()
        StepVerifier.create(findAll)
            .expectNextCount(3L)
            .verifyComplete()

        val findByName = reservationRepository.findByName("First")
        StepVerifier.create(findByName)
            .expectNextCount(1L)
            .verifyComplete()
    }
    
}