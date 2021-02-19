package io.mickeckemi21.springtipsexamples.bootifulreactivetesting.producer

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router

@Configuration
class ReservationRestConfig {

    @Bean
    fun routes(reservationRepository: ReservationRepository): RouterFunction<ServerResponse> = router {
        GET("/reservations") {
            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reservationRepository.findAll())
        }
    }

}