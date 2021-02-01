package io.mickeckemi21.springtipsexamples.serversentevents.springmvc

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.file.dsl.Files
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.SubscribableChannel
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.io.File

@SpringBootApplication
@RestController
class ServerSentEventsSpringWebFluxApplication {

    @Bean
    fun filesChannel(): SubscribableChannel = MessageChannels.publishSubscribe().get()

    @GetMapping("/files/{name}", produces = [TEXT_EVENT_STREAM_VALUE])
    fun files(@PathVariable name: String): Flux<String> =
        Flux.create { sink ->
            val messageHandler = MessageHandler { msg ->
                sink.next(msg.payload as String)
            }
            filesChannel().subscribe(messageHandler)
            sink.onCancel { filesChannel().unsubscribe(messageHandler) }
        }


    @Bean
    fun filesInboundFlow(@Value("\${input-dir:in}") inputDir: File): IntegrationFlow =
        IntegrationFlows
            .from(
                Files
                    .inboundAdapter(inputDir)
                    .preventDuplicates(true)
                    .autoCreateDirectory(true)
            ) { endpointConfigurer ->
                endpointConfigurer.poller { pollerMetadata ->
                    pollerMetadata.fixedDelay(1000L)
                }
            }
            .transform(File::class.java, File::getAbsolutePath)
            .channel(filesChannel())
            .get()

}

fun main(args: Array<String>) {
    runApplication<ServerSentEventsSpringWebFluxApplication>(*args)
}
