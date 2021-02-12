package io.mickeckemi21.springtipsexamples.springintegrationkotlindsl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.dsl.integrationFlow
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.file.dsl.Files.inboundAdapter
import java.io.File

fun main(args: Array<String>) {
    runApplication<SpringIntegrationKotlinDslApplication>(*args)
}

@SpringBootApplication
class SpringIntegrationKotlinDslApplication {

    private val input = File("${System.getenv("HOME")}/Desktop/in")
    private val out = File("${System.getenv("HOME")}/Desktop/out")
    private val outCsv = File(out, "csv")
    private val outTxt = File(out, "txt")

    @Configuration
    class ChannelsConfiguration {
        @Bean
        fun csvChannel(): DirectChannel = MessageChannels.direct().get()

        @Bean
        fun txtChannel(): DirectChannel = MessageChannels.direct().get()

        @Bean
        fun errorsChannel(): DirectChannel = MessageChannels.direct().get()
    }

    @Bean
    fun filesFlow(channels: ChannelsConfiguration) = integrationFlow(
        inboundAdapter(input).autoCreateDirectory(true),
        { poller { it.fixedDelay(1000L).maxMessagesPerPoll(1) } }
    ) {
        filter<File> { it.isFile }
        route<File> {
            when (it.extension) {
				"csv" -> channels.csvChannel()
				"txt" -> channels.txtChannel()
				else -> channels.errorsChannel()
            }
        }
    }

    @Bean
    fun csvFlow(channels: ChannelsConfiguration) = integrationFlow(
        channels.csvChannel()
    ) {
        handle(Files.outboundAdapter(outCsv).autoCreateDirectory(true))
    }

    @Bean
    fun txtFlow(channels: ChannelsConfiguration) = integrationFlow(
        channels.txtChannel()
    ) {
        handle(Files.outboundAdapter(outTxt).autoCreateDirectory(true))
    }

}
