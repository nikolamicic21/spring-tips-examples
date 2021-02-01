package io.mickeckemi21.springtipsexamples.serversentevents.springmvc

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.handler.GenericHandler

@SpringBootApplication
@RestController
class ServerSentEventsSpringMvcApplication {

    private val sses = ConcurrentHashMap<String, SseEmitter>()

    @GetMapping("/files/{name}")
    fun files(@PathVariable name: String): SseEmitter =
        SseEmitter(60 * 1000L).also {
            sses[name] = it
        }

    @Bean
    fun fileInboundFlow(@Value("\${input-dir:in}") inputDir: File): IntegrationFlow =
        IntegrationFlows
            .from(
                Files
                    .inboundAdapter(inputDir)
                    .autoCreateDirectory(true)
                    .preventDuplicates(true)
            ) { endpointConfigurer ->
                endpointConfigurer.poller { pollerMetadata ->
                    pollerMetadata.fixedDelay(1000L)
                }
            }
            .transform(File::class.java, File::getAbsolutePath)
            .handle(String::class.java) { path, _ ->
                sses.forEach { (_, sse) -> sse.send(path) }
            }
            .get()

}

fun main(args: Array<String>) {
    runApplication<ServerSentEventsSpringMvcApplication>(*args)
}
