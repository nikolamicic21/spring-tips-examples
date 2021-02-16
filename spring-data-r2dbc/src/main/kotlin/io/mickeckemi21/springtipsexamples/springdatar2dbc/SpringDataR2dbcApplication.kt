package io.mickeckemi21.springtipsexamples.springdatar2dbc

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@SpringBootApplication
class SpringDataR2dbcApplication

fun main(args: Array<String>) {
    runApplication<SpringDataR2dbcApplication>(*args)
}

@Bean
fun connectionFactoryInitializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer =
    ConnectionFactoryInitializer().apply {
        setConnectionFactory(connectionFactory)
        setDatabasePopulator(
            ResourceDatabasePopulator(
                ClassPathResource("schema.sql")
            )
        )
    }

interface SpringDataR2DbcReservationRepository : ReactiveCrudRepository<Reservation, Int> {

    @Query("SELECT * FROM reservation WHERE name = $1")
    fun findByName(name: String): Mono<Reservation>

}

@Repository
class PlainR2DbcReservationRepository(
    private val connectionFactory: ConnectionFactory
) {

    fun findAll(): Flux<Reservation> =
        connection()
            .flatMapMany { connection ->
                Flux.from(connection.createStatement("SELECT * FROM reservation").execute())
                    .flatMap { r ->
                        r.map { row, _ ->
                            Reservation(
                                row.get("id") as Int,
                                row.get("name") as String
                            )
                        }
                    }
            }

    fun save(reservation: Reservation): Flux<Reservation> =
        connection()
            .flatMapMany { connection ->
                connection.createStatement("INSERT INTO reservation(name) VALUES ($1)")
                    .bind("$1", reservation.name)
                    .add()
                    .execute()
            }
            .switchMap {
                Flux.just(
                    Reservation(
                        reservation.id,
                        reservation.name
                    )
                )
            }

    fun deleteById(id: Int): Mono<Void> =
        connection()
            .flatMapMany { connection ->
                connection.createStatement("DELETE FROM reservation WHERE id = $1")
                    .bind("$1", id)
                    .execute()
            }
            .then()

    fun findByName(name: String): Mono<Reservation> =
        connection()
            .flatMap { connection ->
                connection.createStatement("SELECT * FROM reservation WHERE name = $1")
                    .bind("$1", name)
                    .execute()
                    .toMono()
                    .flatMap { result ->
                        result.map { row, _ ->
                            Reservation(
                                row.get("id") as Int,
                                row.get("name") as String
                            )
                        }.toMono()
                    }
            }

    private fun connection(): Mono<Connection> =
        Mono.from(connectionFactory.create())

}

//@Configuration
//class ConnectionFactoryConfig {
//
//    @Bean
//    fun connectionFactory(): ConnectionFactory = H2ConnectionConfiguration
//        .builder()
//        .url("localhost")
//        .password("password")
//        .username("root")
//        .build().let {
//            H2ConnectionFactory(it)
//        }
//
//}

data class Reservation(
    @Id
    val id: Int?,
    val name: String
)

