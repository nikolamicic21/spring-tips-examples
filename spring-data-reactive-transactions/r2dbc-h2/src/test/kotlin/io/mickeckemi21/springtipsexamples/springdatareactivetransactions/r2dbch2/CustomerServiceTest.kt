package io.mickeckemi21.springtipsexamples.springdatareactivetransactions.r2dbch2

import io.mickeckemi21.springtipsexamples.springdatareactivetransactions.CustomerRepository
import io.mickeckemi21.springtipsexamples.springdatareactivetransactions.CustomerService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.test.StepVerifier

@SpringBootTest
internal class CustomerServiceTest {

    @Autowired
    private lateinit var customerService: CustomerService

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Test
    fun saveAll() {
        StepVerifier
            .create(customerRepository.deleteAll())
            .verifyComplete()

        StepVerifier
            .create(customerService.saveAll("jane@jane.com", "jorge@jorge.com", "jeff@jeff.com"))
            .expectNextCount(3L)
            .verifyComplete()

        StepVerifier
            .create(customerRepository.findAll())
            .expectNextCount(3L)
            .verifyComplete()

        StepVerifier
            .create(customerService.saveAll("foo@foo.com", "bar"))
            .expectNextCount(1L)
            .expectError()
            .verify()

        StepVerifier
            .create(customerRepository.findAll())
            .expectNextCount(3L)
            .verifyComplete()
    }

}