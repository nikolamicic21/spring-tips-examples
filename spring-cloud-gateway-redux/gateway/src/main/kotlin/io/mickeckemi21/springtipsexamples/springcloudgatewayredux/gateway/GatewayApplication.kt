package io.mickeckemi21.springtipsexamples.springcloudgatewayredux.gateway

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.cloud.gateway.event.RefreshRoutesResultEvent
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter
import org.springframework.cloud.gateway.filter.factory.SetPathGatewayFilterFactory
import org.springframework.cloud.gateway.filter.ratelimit.PrincipalNameKeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.CachingRouteLocator
import org.springframework.cloud.gateway.route.Route
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicBoolean

@SpringBootApplication
class GatewayApplication {

    private val updateRoutes = AtomicBoolean(false)

    companion object {
        private val log = LoggerFactory.getLogger(GatewayApplication::class.java)
    }

    @Bean
    fun routesRefreshEventListener(): ApplicationListener<RefreshRoutesResultEvent> =
        ApplicationListener<RefreshRoutesResultEvent> {
            log.info(it::class.java.simpleName)
            val crl = it.source as CachingRouteLocator
            crl.routes.subscribe {
                log.info("${it::class.java} : ${it.metadata} : ${it.filters}")
            }
        }

    @Bean
    @RefreshScope
    fun gateway(
        routeLocatorBuilder: RouteLocatorBuilder,
        rateLimiter: RedisRateLimiter
    ): RouteLocator {
        val routes = routeLocatorBuilder.routes()

        routes
            .route { predicateSpec ->
                predicateSpec
                    .path("/hello").and().host("*.spring.io")
                    .filters { gatewayFilterSpec ->
                        gatewayFilterSpec
                            .setPath("/guides")
                    }
                    .uri("http://spring.io")
            }
            .route("twitter") { predicateSpec ->
                predicateSpec
                    .path("/twitter/**")
                    .filters { gatewayFilterSpec ->
                        gatewayFilterSpec
                            .rewritePath("/twitter/(?<user>.*)", "/\${user}")
                    }
                    .uri("http://twitter.com/@")
            }
            .route { predicateSpec -> predicateSpec
                .path("/default")
                .filters { gatewayFilterSpec -> gatewayFilterSpec
                    .filter { exchange, chain ->
                        log.info("This is your second change!")
                        chain.filter(exchange)
                    }
                }
                .uri("https://spring.io/guides")
            }
            .route { predicateSpec ->
                predicateSpec
                    .path("/customers")
                    // circuit breaker
                    .filters { gatewayFilterSpec -> gatewayFilterSpec
                        .circuitBreaker { config -> config.setFallbackUri("forward:/default") }
                    }
                    .uri("lb://customer-service/")
            }.route { predicateSpec ->
                predicateSpec
                    .path("/error/**")
                    .filters { gatewayFilterSpec -> gatewayFilterSpec.retry(5) }
                    .uri("lb://customer-service")
            }.route { predicateSpec -> predicateSpec
                .path("/hello")
                .filters { gatewayFilterSpec -> gatewayFilterSpec
                    .requestRateLimiter { config ->
                        config
                            .setRateLimiter(rateLimiter)
                            .keyResolver = PrincipalNameKeyResolver()
                    }
                }
                .uri("lb://customer-service")
            }

        if (!updateRoutes.get()) {
            updateRoutes.set(true)
            routes.route("refresh-customers") { predicateSpec ->
                predicateSpec
                    .path("/refresh-customers")
                    .uri("lb://customer-service")
            }
        } else {
            routes.route("refresh-customers") { predicateSpec ->
                predicateSpec
                    .path("/refresh-customers")
                    .filters { gatewayFilterSpec -> gatewayFilterSpec.setPath("/ws/customers") }
                    .uri("lb://customer-service")
            }
        }

        return routes.build()
    }

    @Bean
    fun rateLimiter(): RedisRateLimiter =
        RedisRateLimiter(5, 10)

    @Bean
    fun authentication(): MapReactiveUserDetailsService =
        MapReactiveUserDetailsService(
            User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build()
        )

    @Bean
    fun authorization(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain =
        http
            .authorizeExchange { spec ->
                spec.pathMatchers("/hello").authenticated()
                    .anyExchange().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
            .csrf { spec -> spec.disable() }
            .build()

    //    @Bean
    fun gatewayManualConfig(
        filterFactory: SetPathGatewayFilterFactory
    ): RouteLocator {
        val routes = Route.async()
            .id("manual-config-route")
            .asyncPredicate {
                val uri = it.request.uri
                val path = uri.path
                Mono.just(path.contains("/manual-config-customers"))
            }
            .filter(
                OrderedGatewayFilter(
                    filterFactory.apply { it.template = "/customers" },
                    10
                )
            )
            .uri("lb://customer-service/")
            .build()

        return RouteLocator { Flux.just(routes) }
    }

//    @Bean

    fun gatewayRefreshScope(
        routeLocatorBuilder: RouteLocatorBuilder
    ): RouteLocator {
        val routes = routeLocatorBuilder.routes()
        val id = "customers"
        if (!updateRoutes.get()) {
            updateRoutes.set(true)
            routes.route(id) { predicateSpec ->
                predicateSpec
                    .path("/customers")
                    .uri("lb://customer-service")
            }
        } else {
            routes.route(id) { predicateSpec ->
                predicateSpec
                    .path("/customers")
                    .filters { gatewayFilterSpec -> gatewayFilterSpec.setPath("/ws/customers") }
                    .uri("lb://customer-service")
            }
        }

        return routes.build()
    }

}

fun main(args: Array<String>) {
    runApplication<GatewayApplication>()
}
