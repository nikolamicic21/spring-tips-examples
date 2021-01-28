package io.mickeckemi21.springtipsexamples.rsocket.requestresponse

import io.rsocket.*
import io.rsocket.transport.netty.client.TcpClientTransport
import io.rsocket.transport.netty.server.TcpServerTransport
import io.rsocket.util.DefaultPayload
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
import java.time.Instant
import java.util.stream.Stream

@SpringBootApplication
class RequestResponse

fun main(args: Array<String>) {
    runApplication<RequestResponse>(*args)
}

@Component
class Producer : Ordered, ApplicationListener<ApplicationReadyEvent> {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    fun notifications(name: String): Flux<String> =
        Flux.fromStream(Stream.generate { "Hello $name @ ${Instant.now()}" })
            .delayElements(Duration.ofSeconds(1L))

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val acceptor = SocketAcceptor { _, _ ->
            val abstractRSocket = object : AbstractRSocket() {
                override fun requestStream(payload: Payload): Flux<Payload> {
                    val name = payload.dataUtf8

                    return notifications(name)
                        .map(DefaultPayload::create)
                }
            }

            Mono.just(abstractRSocket)
        }
        val transport = TcpServerTransport.create(7000)

        RSocketFactory
            .receive()
            .acceptor(acceptor)
            .transport(transport)
            .start()
            .block()
    }

}

@Component
class Consumer : Ordered, ApplicationListener<ApplicationReadyEvent> {

    companion object {
        private val log = LoggerFactory.getLogger(Consumer::class.java)
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val transport = TcpClientTransport.create(7000)
        RSocketFactory
            .connect()
            .transport(transport)
            .start()
            .flatMapMany {
                it.requestStream(DefaultPayload.create("Spring Tips"))
                    .map(Payload::getDataUtf8)
                    .doOnNext(log::info)
            }
            .subscribe { log.info("Processing new result $it") }

    }
}