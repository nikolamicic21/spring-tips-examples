package io.mickeckemi21.springtipsexamples.reactorcontext.mdc

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Signal
import reactor.core.scheduler.Schedulers
import reactor.util.context.Context
import java.util.concurrent.ConcurrentSkipListSet
import java.util.function.Consumer
import kotlin.random.Random
import java.rmi.server.UID
import java.util.concurrent.Executors


fun main() {
    runApplication<MdcApp>()
}

@SpringBootApplication
@RestController
class MdcApp(private val restaurantService: RestaurantService) {

    companion object {
        private const val UID = "uid"
        private val log = LoggerFactory.getLogger(MdcApp::class.java)
    }

    @GetMapping("/{uid}/restaurants/{price}")
    fun getRestaurants(@PathVariable uid: String, @PathVariable price: Double): Flux<Restaurant> =
        adaptResults(restaurantService.getByPriceLessThan(price), uid)

    private fun adaptResults(input: Flux<Restaurant>, uid: String): Flux<Restaurant> =
        Mono.just("finding restaurants for $uid")
            .doOnEach { log.info(it.toString()) }
            .thenMany(input)
            .doOnEach(logOnNext { restaurant -> log.info("found restaurant ${restaurant.name} for \$${restaurant.pricePerPerson}") })
            .contextWrite(Context.of(mutableMapOf(UID to uid)))

    private fun <T> logOnNext(logStatement: Consumer<T>): Consumer<Signal<T>> = Consumer { signal ->
        if (signal.isOnNext) {
            val uidOptional = signal.contextView.getOrEmpty<String>(UID)
            if (uidOptional.isPresent) {
                val uid = uidOptional.get()
                MDC.putCloseable(UID, uid).use {
                    logStatement.accept(signal.get()!!)
                }
            } else {
                logStatement.accept(signal.get()!!)
            }
        }
    }

}

@Service
class RestaurantService {

    private val restaurants = ConcurrentSkipListSet<Restaurant> { o1, o2 ->
        o1.pricePerPerson.compareTo(o2.pricePerPerson)
    }

    init {
        (0..1000)
            .map { "restaurant #$it" }
            .map { Restaurant(Random.nextDouble() * 100, it) }
            .forEach(restaurants::add)
    }

    fun getByPriceLessThan(price: Double): Flux<Restaurant> =
        Flux.fromStream(
            restaurants.parallelStream()
                .filter { it.pricePerPerson <= price }
        )

}

data class Restaurant(
    val pricePerPerson: Double,
    val name: String
)