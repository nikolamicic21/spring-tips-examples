package io.mickeckemi21.springtipsexamples.bootifuldashboards.clienta

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@EnableDiscoveryClient
@RestController
class ClientAApplication {

    @GetMapping("/", "")
    fun get(): String = "a"

}

fun main(args: Array<String>) {
    runApplication<ClientAApplication>(*args)
}
