package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import reactor.core.publisher.Flux

@SpringBootTest(
    properties = ["server.port=0"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(
    ReservationRestConfig::class
)
internal class BaseClass {

    @LocalServerPort
    private var port: Int? = null

    @MockBean
    private lateinit var reservationRepository: ReservationRepository

    @BeforeEach
    fun setUp() {
        Mockito
            .`when`(reservationRepository.findAll())
            .thenReturn(Flux.just(Reservation("1", "A"), Reservation("2", "B")))
        RestAssured.baseURI = "http://localhost:$port"
    }
}