package io.mickeckemi21.springtipsexamples.springcloudstream

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.annotation.Input
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Scope
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.messaging.SubscribableChannel

@SpringBootApplication
@EnableBinding(ConsumerChannels::class)
class SpringCloudStreamConsumerApplication {

	@Bean
	@Scope("prototype")
	fun logger(injectionPoint: InjectionPoint): Logger =
		LoggerFactory.getLogger(injectionPoint.declaredType.name)

	@Bean
	fun integrationFlow(
		channels: ConsumerChannels,
		logger: Logger
	): IntegrationFlow = IntegrationFlows
		.from(channels.producer())
		.handle(String::class.java) { payload, _ ->
			logger.info("new message: $payload")
		}
		.get()

}

interface ConsumerChannels {

	@Input
	fun producer(): SubscribableChannel

}

fun main(args: Array<String>) {
	runApplication<SpringCloudStreamConsumerApplication>(*args)
}
