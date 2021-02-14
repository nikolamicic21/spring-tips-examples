package io.mickeckemi21.springtipsexamples.bootifultesting

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@WebMvcTest
internal class ReservationRestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var reservationRepository: ReservationRepository

    @Test
    fun getReservations() {
        Mockito.`when`(reservationRepository.findAll())
            .thenReturn(listOf(Reservation(1L, "Jane")))

        mockMvc.perform(MockMvcRequestBuilders.get("/reservations"))
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.jsonPath("@.[0].id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("@.[0].name").value("Jane"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

}