package io.mickeckemi21.springtipsexamples.springkotlincoroutines

import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.context.support.beans
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.DatabasePopulator
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.flow
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.time.Instant

@SpringBootApplication
class SpringKotlinCoroutinesApplication

fun note(msg: String) = println("${Thread.currentThread().name}: $msg ${Instant.now()}")

fun main() = runBlocking {
    fun `1`() {
        val job = GlobalScope.launch {
            delay(1_000L)
            note("the end of 1()")
        }
        note("the start of 1()")
    }

    fun `2`() {
        note("2(): start")
        runBlocking {
            note("2(): blocking start")
            delay(2_000L)
            note("2(): blocking end")
        }
        note("2(): stop")
    }

    fun `3`() {
        val deferred = (1..1_000_000).map { num ->
            GlobalScope.async {
                delay(1_000)
                num
            }
        }
        runBlocking {
            note("before sum...")
            val sum = deferred.sumBy { it.await() }
            note("after sum... : $sum")
        }
    }

    fun `4`() {
        val ints: Flow<Int> = flow {
            (1..10).forEach {
                delay(1_000L)
                emit(it)
            }
        }
        runBlocking {
            ints.collect { note("$it") }
        }
    }

    fun `5`() {

        data class Reservation(val id: Int, val name: String)

        class ReservationRepository(private val databaseClient: DatabaseClient) {

            suspend fun findOne(name: String): Reservation? = databaseClient
                .sql("SELECT * FROM reservation WHERE name = :name")
                .bind("name", name)
                .map { row ->
                    Reservation(
                        row.get("id") as Int,
                        row.get("name") as String
                    )
                }
                .awaitOneOrNull()

            fun all(): Flow<Reservation> = databaseClient
                .sql("SELECT * FROM reservation")
                .map { row ->
                    Reservation(
                        row.get("id") as Int,
                        row.get("name") as String
                    )
                }
                .flow()

        }

        val bdDsl: BeanDefinitionDsl = beans {
            bean {
                ConnectionFactoryInitializer().apply {
                    setConnectionFactory(ref())
                    setDatabasePopulator(
                        ResourceDatabasePopulator(
                            ClassPathResource("schema.sql"),
                            ClassPathResource("data.sql")
                        )
                    )
                }
            }
            bean {
                ReservationRepository(ref())
            }
            bean {
                val reservationReadable = ref<ReservationRepository>()
                coRouter {
                    GET("/reservations") {
                        ServerResponse.ok().bodyAndAwait(reservationReadable.all())
                    }
                    GET("/reservations/{name}") { request ->
                        val reservation = reservationReadable.findOne(request.pathVariable("name"))
                            ?: throw IllegalArgumentException("No reservation for provided name")
                        ServerResponse.ok().bodyValueAndAwait(reservation)
                    }
                }
            }
        }
        runApplication<SpringKotlinCoroutinesApplication> {
            addInitializers(bdDsl)
            setDefaultProperties(mapOf("server.port" to 8082))
        }
    }
    `5`()

    delay(20_000L)
}
