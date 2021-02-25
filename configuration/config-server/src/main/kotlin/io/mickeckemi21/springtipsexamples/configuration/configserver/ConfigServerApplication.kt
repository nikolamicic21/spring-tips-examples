package io.mickeckemi21.springtipsexamples.configuration.configserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer

@SpringBootApplication
@EnableConfigServer
class ConfigServerApplication

fun main(args: Array<String>) {
	System.setProperty("spring.profiles.active", "native")
	runApplication<ConfigServerApplication>(*args)
}
