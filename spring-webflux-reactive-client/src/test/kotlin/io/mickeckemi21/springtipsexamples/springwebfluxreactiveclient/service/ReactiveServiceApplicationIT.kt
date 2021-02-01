package io.mickeckemi21.springtipsexamples.springwebfluxreactiveclient.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
internal class ReactiveServiceApplicationIT {

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun before() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:8080")
            .build()
    }

    @Test
    fun eventById() {
        webTestClient.get()
            .uri("/events/42")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
    }

}
