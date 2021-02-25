package io.mickeckemi21.springtipsexamples.organizationalconsistency.numbers

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "numbers")
@ConstructorBinding
data class NumberProperties(val bound: Int)