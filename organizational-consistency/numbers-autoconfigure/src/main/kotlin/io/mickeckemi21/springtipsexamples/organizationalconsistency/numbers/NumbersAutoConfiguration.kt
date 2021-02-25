package io.mickeckemi21.springtipsexamples.organizationalconsistency.numbers

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import java.util.concurrent.ThreadLocalRandom

@Configuration
@EnableConfigurationProperties(NumberProperties::class)
@ConditionalOnClass(ThreadLocalRandom::class)
@Import(NumbersRouterConfiguration::class)
class NumbersAutoConfiguration {

    @Bean
    fun numberService(numberProperties: NumberProperties): NumberService =
        NumberService(numberProperties)

}
