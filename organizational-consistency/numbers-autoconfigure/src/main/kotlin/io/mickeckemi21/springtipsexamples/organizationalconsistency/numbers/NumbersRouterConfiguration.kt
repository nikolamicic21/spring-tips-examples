package io.mickeckemi21.springtipsexamples.organizationalconsistency.numbers

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
@ConditionalOnClass(RouterFunction::class)
class NumbersRouterConfiguration {

    @Bean
    fun routes(numberService: NumberService): RouterFunction<ServerResponse> = router {
        GET("/number") {
            ServerResponse.ok().bodyValue(mapOf("number" to numberService.generateRandomNumber()))
        }
    }

}