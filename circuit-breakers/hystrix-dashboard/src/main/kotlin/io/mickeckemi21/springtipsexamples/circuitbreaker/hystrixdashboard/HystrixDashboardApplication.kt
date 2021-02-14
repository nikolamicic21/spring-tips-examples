package io.mickeckemi21.springtipsexamples.circuitbreaker.hystrixdashboard

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard

@SpringBootApplication
@EnableHystrixDashboard
class HystrixDashboardApplication

fun main(args: Array<String>) {
    runApplication<HystrixDashboardApplication>(*args)
}
