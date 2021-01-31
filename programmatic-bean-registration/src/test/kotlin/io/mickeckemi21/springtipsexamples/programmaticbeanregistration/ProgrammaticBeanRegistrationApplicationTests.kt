package io.mickeckemi21.springtipsexamples.programmaticbeanregistration

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
//@ExtendWith(SpringExtension::class)
//@ContextConfiguration(classes = [ProgrammaticBeanRegistrationApplication::class])
class ProgrammaticBeanRegistrationApplicationTests {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun contextLoads() {
        assertNotNull(context)
        assertNotNull(context.getBean(BarService::class.java))
        assertNotNull(context.getBean(FooService::class.java))
    }

}
