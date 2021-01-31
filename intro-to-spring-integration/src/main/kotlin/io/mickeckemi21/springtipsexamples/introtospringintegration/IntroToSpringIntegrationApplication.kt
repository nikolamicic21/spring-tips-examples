package io.mickeckemi21.springtipsexamples.introtospringintegration

import org.springframework.amqp.core.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ImageBanner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.io.FileSystemResource
import org.springframework.integration.amqp.dsl.Amqp
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.file.FileHeaders
import org.springframework.integration.file.FileNameGenerator
import org.springframework.integration.file.dsl.Files
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.ftp.dsl.Ftp
import org.springframework.integration.ftp.outbound.FtpMessageHandler
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory
import org.springframework.integration.handler.GenericHandler
import org.springframework.integration.transformer.GenericTransformer
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.support.MessageBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

const val ASCII = "ascii"

@SpringBootApplication
class IntroToSpringIntegrationApplication {

    @Bean
    fun fromFileWithImagesToAsciiPubSubChannel(
        @Value("\${in-dir:in}") inDir: File,
        env: Environment
    ): IntegrationFlow {
        val imageToTextImageTransformer = GenericTransformer<File, Message<String>> { source ->
            val baos = ByteArrayOutputStream()
            baos.use {
                PrintStream(baos).use {
                    val imageBanner = ImageBanner(FileSystemResource(source!!))
                    imageBanner.printBanner(env, javaClass, it)
                }
            }
            return@GenericTransformer MessageBuilder
                .withPayload(String(baos.toByteArray()))
                .setHeader(FileHeaders.FILENAME, source.absoluteFile.name)
                .build()
        }

        return IntegrationFlows
            .from(
                Files
                    .inboundAdapter(inDir)
                    .autoCreateDirectory(true)
                    .preventDuplicates(true)
                    .patternFilter("*.jpg")
            ) { poller ->
                poller.poller { pollerMetadata ->
                    pollerMetadata.fixedDelay(1000L)
                }
            }
            .transform(imageToTextImageTransformer)
            .channel(asciiProcessors())
            .get()
    }

    @Bean
    fun asciiProcessors(): MessageChannel =
        MessageChannels.publishSubscribe().get()

    @Bean
    fun fromPubSubChannelToFtp(ftpSessionFactory: DefaultFtpSessionFactory): IntegrationFlow = IntegrationFlows
        .from(asciiProcessors())
        .handle(Ftp
            .outboundAdapter(ftpSessionFactory)
            .autoCreateDirectory(true)
            .remoteDirectory("uploads")
            .fileNameGenerator { message ->
                val filename = message.headers[FileHeaders.FILENAME] as String
                "${filename.split(".")[0]}.txt"
            }
            .get()
        )
        .get()

    @Bean
    fun ftpSessionFactory(
        @Value("\${ftp.host:localhost}") host: String,
        @Value("\${ftp.port:21}") port: Int,
        @Value("\${ftp.username:user}") username: String,
        @Value("\${ftp.password:password}") password: String,
    ): DefaultFtpSessionFactory =
        DefaultFtpSessionFactory().apply {
            setHost(host)
            setPort(port)
            setUsername(username)
            setPassword(password)
            setClientMode(2)
        }

    @Bean
    fun amqp(amqpTemplate: AmqpTemplate): IntegrationFlow = IntegrationFlows
        .from(asciiProcessors())
        .handle(
            Amqp
                .outboundAdapter(amqpTemplate)
                .exchangeName(ASCII)
                .routingKey(ASCII)
                .get()
        )
        .get()

    @Bean
    fun exchange(): Exchange = ExchangeBuilder
        .directExchange(ASCII)
        .durable(true)
        .build()

    @Bean
    fun queue(): Queue = QueueBuilder
        .durable(ASCII)
        .build()

    @Bean
    fun binding(): Binding = BindingBuilder
        .bind(queue())
        .to(exchange())
        .with(ASCII)
        .noargs()

}

fun main(args: Array<String>) {
    runApplication<IntroToSpringIntegrationApplication>(*args)
}
