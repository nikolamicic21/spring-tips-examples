package io.mickeckemi21.springtipsexamples.bootifuldashboards.clientb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@EnableDiscoveryClient
@RestController
class ClientBApplication {

    @GetMapping("/", "")
    fun get(): String = "b"

}

fun main(args: Array<String>) {
    runApplication<ClientBApplication>(*args)
}
