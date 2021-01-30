package io.mickeckemi21.springtipsexamples.rsocketspringsecurity.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity
import org.springframework.security.config.annotation.rsocket.RSocketSecurity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream
import java.util.stream.Stream.generate

@SpringBootApplication
class ServiceApplication

fun main(args: Array<String>) {
    runApplication<ServiceApplication>(*args)
}

@Configuration
@EnableRSocketSecurity
class RSocketSecurityConfig {

    @Bean
    fun messageHandler(strategies: RSocketStrategies): RSocketMessageHandler =
        RSocketMessageHandler().apply {
            argumentResolverConfigurer.addCustomResolver(AuthenticationPrincipalArgumentResolver())
            rSocketStrategies = strategies
        }

    @Bean
    fun authentication(): MapReactiveUserDetailsService {
        val user = User.withDefaultPasswordEncoder()
            .username("user")
            .password("pass")
            .roles("USER")
            .build()
        val admin = User.withDefaultPasswordEncoder()
            .username("admin")
            .password("pass")
            .roles("USER", "ADMIN")
            .build()

        return MapReactiveUserDetailsService(user, admin)
    }

    @Bean
    fun authorization(security: RSocketSecurity): PayloadSocketAcceptorInterceptor =
        security
            .authorizePayload { authPayloadSpec ->
                authPayloadSpec.route("greetings").authenticated()
                    .anyExchange().permitAll()
            }
            .simpleAuthentication(Customizer.withDefaults())
            .build()

}

class GreetingResponse() {
    var message: String? = null

    constructor(message: String) : this() {
        this.message = message
    }
}

@Controller
class GreetingService {

    @MessageMapping("greetings")
    fun greet(@AuthenticationPrincipal user: Mono<UserDetails>): Flux<GreetingResponse> = user
        .map(UserDetails::getUsername)
        .flatMapMany { principalName -> greet(principalName) }

}

fun greet(name: String): Flux<GreetingResponse> =
    Flux.fromStream(generate { GreetingResponse("Hello $name @ ${Instant.now()}") })
        .delayElements(Duration.ofSeconds(1L))
