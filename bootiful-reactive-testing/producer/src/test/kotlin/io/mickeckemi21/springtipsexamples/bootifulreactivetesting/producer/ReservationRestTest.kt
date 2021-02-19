package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux

@WebFluxTest
@Import(ReservationRestConfig::class)
internal class ReservationRestTest {

    @Autowired
    private lateinit var client: WebTestClient

    @MockBean
    private lateinit var reservationRepository: ReservationRepository

    @Test
    fun getAllReservations() {
        Mockito.`when`(reservationRepository.findAll())
            .thenReturn(Flux.just(Reservation("1", "A"), Reservation("2", "B")))

        client
            .get()
            .uri("http://localhost:8080/reservations")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_JSON_VALUE)
            .expectBody()
            .jsonPath("@.[0].id").isEqualTo("1")
            .jsonPath("@.[0].name").isEqualTo("A")
            .jsonPath("@.[1].id").isEqualTo("2")
            .jsonPath("@.[1].name").isEqualTo("B")
    }

}