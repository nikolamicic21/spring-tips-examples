package io.mickeckemi21.springtipsexamples.springcloudgatewayredux.configserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.cloud.config.server.EnableConfigServer

@SpringBootApplication
@EnableConfigServer
class ConfigServerApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder().profiles("native")
        .sources(ConfigServerApplication::class.java)
        .run(*args)
}
