package io.mickeckemi21.springtipsexamples.reactorcontext.simple

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.util.context.Context
import java.util.*
import java.util.concurrent.Executors

fun main() {
    runApplication<SimpleApp>()
}

@RestController
@SpringBootApplication
class SimpleApp {

    companion object {
        private val SCHEDULER = Schedulers.fromExecutor(Executors.newFixedThreadPool(10))
        private val log = LoggerFactory.getLogger(SimpleApp::class.java)
    }

    fun <T> Flux<T>.prepare(): Flux<T> {
        return this
            .doOnNext { log.info(it.toString()) }
            .subscribeOn(SCHEDULER)
    }

    fun read(): Flux<String> {
        val letters = Flux.just("A", "B", "C").prepare()
        val numbers = Flux.just(1, 2, 3).prepare()
        return Flux.zip(letters, numbers)
            .map { tpl -> "${tpl.t1} : ${tpl.t2}" }
            .doOnEach { signal ->
                if (signal.isOnNext) {
                    val contextView = signal.contextView
                    val userId = contextView.get<String>("userId")
                    log.info("user id for this pipeline stage for data '${signal.get()!!}' is '$userId'")
                }
            }
            .contextWrite(Context.of(mutableMapOf("userId" to UUID.randomUUID().toString())))
            .prepare()
    }

    @GetMapping("/data")
    fun get(): Flux<String> = read()

}