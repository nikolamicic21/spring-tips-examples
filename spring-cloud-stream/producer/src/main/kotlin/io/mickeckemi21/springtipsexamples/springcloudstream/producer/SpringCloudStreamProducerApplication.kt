package io.mickeckemi21.springtipsexamples.springcloudstream.producer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Output
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
@EnableBinding(ProducerChannels::class)
class SpringCloudStreamProducerApplication(private val channels: ProducerChannels) {

	@PostMapping("/greet/{name}")
	fun publishGreet(@PathVariable name: String) {
		val greeting = "Hello, $name!"
		val message = MessageBuilder.withPayload(greeting).build()
		channels.consumer().send(message)
	}

}

interface ProducerChannels {

	@Output
	fun consumer(): MessageChannel

}

fun main(args: Array<String>) {
	runApplication<SpringCloudStreamProducerApplication>(*args)
}
