package io.mickeckemi21.springtipsexamples.springbootplugin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringBootPluginApplication

fun main(args: Array<String>) {
    runApplication<SpringBootPluginApplication>(*args)
}
