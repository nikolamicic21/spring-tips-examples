package io.mickeckemi21.springtipsexamples.springdatareactivetransactions

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.util.Assert
import reactor.core.publisher.Flux

@SpringBootApplication
@EnableTransactionManagement
class ReactiveMongodbApplication {

    @Bean
    fun transactionalOperator(
        reactiveTransactionManager: ReactiveTransactionManager
    ): TransactionalOperator =
        TransactionalOperator.create(reactiveTransactionManager)

    @Bean
    fun reactiveTransactionManager(
        reactiveMongoDatabaseFactory: ReactiveMongoDatabaseFactory
    ): ReactiveTransactionManager =
        ReactiveMongoTransactionManager(reactiveMongoDatabaseFactory)

}

fun main(args: Array<String>) {
    runApplication<ReactiveMongodbApplication>(*args)
}

@Service
class CustomerService(
    private val transactionalOperator: TransactionalOperator,
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

@Document
data class Customer(
    @Id
    val id: String?,
    val email: String
)
