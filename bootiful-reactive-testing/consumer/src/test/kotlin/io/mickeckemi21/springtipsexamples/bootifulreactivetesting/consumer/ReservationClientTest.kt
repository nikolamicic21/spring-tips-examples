package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.consumer

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties
import org.springframework.util.StringUtils
import reactor.test.StepVerifier
import java.util.function.Predicate

@SpringBootTest
//@AutoConfigureWireMock(port = 8080)
@AutoConfigureStubRunner(
    ids = ["io.mickeckemi21:producer:+:8080"],
    repositoryRoot = "stubs://file://<PATH_TO_STUBS_DIRECTORY>/",
    stubsMode = StubRunnerProperties.StubsMode.REMOTE
)
class ReservationClientTest {

    @Autowired
    private lateinit var reservationClient: ReservationClient

    @Test
    fun getAllReservations() {
//        WireMock.stubFor(
//            WireMock
//                .get(
//                    WireMock.urlMatching("/reservations")
//                )
//                .willReturn(
//                    WireMock.aResponse()
//                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                        .withStatus(HttpStatus.OK.value())
//                        .withBody("[{\"id\":\"1\",\"name\":\"Jane\"},{\"id\":\"2\",\"name\":\"Bob\"}]")
//                )
//        )

        val allReservation = reservationClient.getAllReservation()

        val predicate = Predicate<Reservation> { r ->
            StringUtils.hasText(r.id) && StringUtils.hasText(r.name)
        }

        StepVerifier.create(allReservation)
            .expectNextMatches(predicate)
            .expectNextMatches(predicate)
            .verifyComplete()
    }

}