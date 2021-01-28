package io.mickeckemi21.springtipsexamples.rsocket.channel

import io.rsocket.*
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@SpringBootApplication
class PingPong

fun main(args: Array<String>) {
    runApplication<PingPong>(*args)
}

fun reply(input: String): String {
    if (input.contentEquals("ping")) {
        return "pong"
    }
    if (input.contentEquals("pong")) {
        return "ping"
    }

    throw IllegalArgumentException()
}

@Component
class Ping : Ordered, ApplicationListener<ApplicationReadyEvent> {

    companion object {
        private val log = LoggerFactory.getLogger(Ping::class.java)
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val transport = TcpClientTransport.create(7001)
        val client = RSocketFactory
            .connect()
            .transport(transport)
            .start()


        client.flatMapMany { socket ->
                socket.requestChannel(Flux
                    .interval(Duration.ofSeconds(1L))
                    .map { DefaultPayload.create("ping") }
                )
                .map(Payload::getDataUtf8)
                .doOnNext { received -> log.info("received $received in ${javaClass.name}") }
                .take(10)
                .doFinally { socket.dispose() }
            }
            .then()
            .block()
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}

@Component
class Pong : SocketAcceptor, Ordered, ApplicationListener<ApplicationReadyEvent> {

    companion object {
        private val log = LoggerFactory.getLogger(Pong::class.java)
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        RSocketFactory
            .receive()
            .acceptor(this)
            .transport(TcpServerTransport.create(7001))
            .start()
            .subscribe()
    }

    override fun accept(setup: ConnectionSetupPayload?, sendingSocket: RSocket?): Mono<RSocket> {
        val rsocket = object : AbstractRSocket() {
            override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
                return Flux.from(payloads)
                    .map(Payload::getDataUtf8)
                    .doOnNext { received -> log.info("received $received in ${javaClass.name}") }
                    .map { received -> reply(received) }
                    .map(DefaultPayload::create)
            }
        }

        return Mono.just(rsocket)
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}