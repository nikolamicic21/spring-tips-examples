package io.mickeckemi21.springtipsexamples.springapachecamel

import org.apache.camel.Component
import org.apache.camel.RoutesBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.file.GenericFile
import org.apache.camel.component.jms.JmsComponent
import org.apache.camel.model.RouteBuilderDefinition
import org.apache.camel.spi.ComponentCustomizer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.integration.dsl.MessageChannels
import org.springframework.integration.handler.GenericHandler
import org.springframework.messaging.MessageChannel
import java.io.*
import java.util.*
import java.util.stream.Collectors
import javax.jms.ConnectionFactory

fun main(args: Array<String>) {
	runApplication<SpringApacheCamelApplication>(*args)
}

@SpringBootApplication
class SpringApacheCamelApplication {

	companion object {
		private val log = LoggerFactory.getLogger(SpringApacheCamelApplication::class.java)
	}

	@Bean
	fun defaultJmsComponentCustomizer(jmsConnectionFactory: ConnectionFactory) : ComponentCustomizer =
		ComponentCustomizer.builder(JmsComponent::class.java)
			.build { jmsComponent -> jmsComponent.connectionFactory = jmsConnectionFactory }

	@Bean
	fun routes(): RouteBuilder = object : RouteBuilder() {
		override fun configure() {
			from("file://{{user.home}}/Desktop/out")
				.routeId("out-to-in")
				.to("file://{{user.home}}/Desktop/in?autoCreate=false")

			from("file://{{user.home}}/Desktop/to-jms")
				.routeId("file-to-jms")
				.transform()
				.body(GenericFile::class.java) { genericFile ->
					val file = genericFile.file as File
					FileInputStream(file).use { fis ->
						InputStreamReader(fis).use { isr ->
							BufferedReader(isr).use { br ->
								br.lines().collect(Collectors.joining())
							}
						}
					}
				}
				.process()
//				.message { msg -> log.info("file-to-jms message is: $message") }
				.body(String::class.java) { text -> log.info("file-to-jms body text is: $text") }
//				.exchange { exchange ->
//					val inMessage = exchange.`in`
//					val body = inMessage.body as String
//					log.info("file-to-jms body is: $body")
//				}
				//@formatter:off
				.choice()
					.`when` { exchange -> (exchange.`in`.body as String).contains("hello") }
					.to("jms:queue:hello")
				.otherwise()
					.to("jms:queue:files")
				.endChoice()
				//@formatter:on

			from("jms:hello")
				.to("spring-integration:incoming")
			
			from("jms:queue:files")
				.routeId("jms-to-file")
				.setHeader("CamelFileName") { UUID.randomUUID().toString() + ".txt" }
				.to("file://{{user.home}}/Desktop/from-jms")
		}
	}

}

@Configuration
class SpringIntegrationFlow {

	companion object {
		private val log = LoggerFactory.getLogger(SpringIntegrationFlow::class.java)
	}

	@Bean
	fun incoming(): MessageChannel = MessageChannels.direct().get()

	@Bean
	fun integrationFlow(): IntegrationFlow = IntegrationFlows
		.from(incoming())
		.handle { payload: Any, _ ->
			log.info("incoming message: $payload")
			null
		}
		.get()

}