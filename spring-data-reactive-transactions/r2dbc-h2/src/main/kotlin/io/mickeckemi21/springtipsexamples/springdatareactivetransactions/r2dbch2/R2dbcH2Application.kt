package io.mickeckemi21.springtipsexamples.springdatareactivetransactions

import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.stereotype.Service
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import reactor.core.publisher.Flux

@SpringBootApplication
@EnableTransactionManagement
class R2dbcH2Application {

    @Bean
    fun connectionFactoryInitializer(
        connectionFactory: ConnectionFactory
    ): ConnectionFactoryInitializer =
        ConnectionFactoryInitializer().apply {
            setConnectionFactory(connectionFactory)
            setDatabasePopulator(ResourceDatabasePopulator(
                ClassPathResource("schema.sql")
            ))
        }

    @Bean
    fun reactiveTransactionManager(
        connectionFactory: ConnectionFactory
    ): ReactiveTransactionManager =
        R2dbcTransactionManager(connectionFactory)

}

fun main(args: Array<String>) {
    runApplication<R2dbcH2Application>(*args)
}

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {

    @Transactional
    fun saveAll(vararg names: String): Flux<Customer> {
        val records = Flux.just(*names)
            .map { Customer(null, it) }
            .flatMap { customerRepository.save(it) }
            .doOnNext {
                Assert.isTrue(
                    it.email.contains("@"),
                    "The email must contain '@' char!"
                )
            }
// 2.           .`as`(transactionalOperator::transactional)

/* 3. */    return records
// 1.       return transactionalOperator.execute{ records }
    }

}

interface CustomerRepository : ReactiveCrudRepository<Customer, String>

data class Customer(
    @Id
    val id: String?,
    val email: String
)
