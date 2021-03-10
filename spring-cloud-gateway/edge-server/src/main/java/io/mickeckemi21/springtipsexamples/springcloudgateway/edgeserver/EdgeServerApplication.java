package io.mickeckemi21.springtipsexamples.springcloudgateway.edgeserver;

import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.IntStream;

@Slf4j
@SpringBootApplication
public class EdgeServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EdgeServerApplication.class, args);
    }

    @Bean
    public RouteLocator routeLocator(
            RouteLocatorBuilder builder,
            RequestRateLimiterGatewayFilterFactory rateLimiterFilterFactory,
            RedisRateLimiter rateLimiter
    ) {
        return builder.routes()
                // basic proxy
                .route("start", predicateSpec -> predicateSpec
                        .path("/start/**")
                        .filters(uriSpec -> uriSpec
                                .stripPrefix(1)
                        )
                        .uri("http://start.spring.io:80/"))
                // load-balanced proxy
                .route("customers-lb", predicateSpec -> predicateSpec
                        .path("/customers/**")
                        .uri("lb://CUSTOMER-SERVICE/")
                )
                // custom filter 1
                .route("custom-filter-1", predicateSpec -> predicateSpec
                        .path("/custom-filter-1")
                        .filters(uriSpec -> uriSpec
                                .filter((exchange, chain) -> chain.filter(exchange)
                                        .then(Mono.fromRunnable(() -> {
                                            final ServerHttpResponse response = exchange.getResponse();
                                            response.setStatusCode(HttpStatus.CONFLICT);
                                            response.getHeaders().setContentType(MediaType.APPLICATION_PDF);
                                        }))))
                        .uri("http://localhost:9090/customers")
                )
                // custom filter 2
                .route("custom-filter-2", predicateSpec -> predicateSpec
                        .path("/custom-filter-2/**")
                        .filters(uriSpec -> uriSpec
                                .rewritePath("/custom-filter-2/(?<CID>.*)", "/customers/${CID}")
                        )
                        .uri("lb://CUSTOMER-SERVICE/")
                )
                // Hystrix circuit breaker
                .route("circuit-breaker", predicateSpec -> predicateSpec
                        .path("/circuit-breaker")
                        .filters(uriSpec -> uriSpec
                                .rewritePath("/circuit-breaker", "/delay")
                                .hystrix(hystrixConfig -> hystrixConfig
                                        .setName("circuit-breaker")
                                        .setSetter(HystrixObservableCommand.Setter
                                                .withGroupKey(() -> "circuit-breaker")
                                                .andCommandKey(() -> "circuit-breaker")
                                                .andCommandPropertiesDefaults(HystrixCommandProperties
                                                        .defaultSetter()
                                                        .withExecutionTimeoutInMilliseconds(5000)
                                                )
                                        )
                                )
                        )
                        .uri("lb://CUSTOMER-SERVICE/")
                )
                // Redis Rate limiter
                .route("rate-limiter", predicateSpec -> predicateSpec
                        .path("/rate-limiter")
                        .filters(uriSpec -> uriSpec
                                .rewritePath("/rate-limiter", "/customers")
                                .filter(rateLimiterFilterFactory.apply(
                                        rateLimiterFilterFactory.newConfig().setRateLimiter(rateLimiter))
                                ))
                        .uri("lb://CUSTOMER-SERVICE/")
                )
                .build();
    }

    @Bean
    public RedisRateLimiter rateLimiter(ApplicationContext context) {
        final RedisRateLimiter rateLimiter = new RedisRateLimiter(5, 10);
        rateLimiter.setApplicationContext(context);
        return rateLimiter;
    }

    @Bean
    public ApplicationRunner client() {
        return args -> {
            WebClient client = WebClient.builder()
                    .filter(ExchangeFilterFunctions.basicAuthentication("user", "pw")).build();
            Flux.fromStream(IntStream.range(0, 100).boxed())
                    .flatMap(number -> client.get().uri("http://localhost:9090/rate-limiter").exchange())
                    .flatMap(clientResponse -> clientResponse
                            .toEntity(String.class)
                            .map(re -> String.format("status: %s; body: %s", re.getStatusCodeValue(), re.getBody())))
                    .subscribe(log::warn);
        };
    }

    // authentication
    @Bean
    public MapReactiveUserDetailsService authentication() {
        return new MapReactiveUserDetailsService(
                User.withDefaultPasswordEncoder()
                        .username("user")
                        .password("pw")
                        .roles("USER")
                        .build()
        );
    }

    // authorization
    @Bean
    public SecurityWebFilterChain authorization(ServerHttpSecurity security) {
        return security
                .authorizeExchange().pathMatchers("/rate-limiter").authenticated()
                .anyExchange().permitAll()
                .and()
                .httpBasic()
                .and()
                .build();
    }

    @Bean
    public DiscoveryClientRouteDefinitionLocator definitionLocator(
            DiscoveryClient discoveryClient,
            DiscoveryLocatorProperties discoveryLocatorProperties
    ) {
        return new DiscoveryClientRouteDefinitionLocator(
                discoveryClient,
                discoveryLocatorProperties
        );
    }

}
