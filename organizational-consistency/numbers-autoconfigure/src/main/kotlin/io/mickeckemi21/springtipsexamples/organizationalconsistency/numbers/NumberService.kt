package io.mickeckemi21.springtipsexamples.organizationalconsistency.numbers

import java.util.concurrent.ThreadLocalRandom

class NumberService(numberProperties: NumberProperties) {

    private val bound = numberProperties.bound

    fun generateRandomNumber(): Int {
        val threadLocalRandom = ThreadLocalRandom.current()
        return threadLocalRandom.nextInt(bound)
    }
}