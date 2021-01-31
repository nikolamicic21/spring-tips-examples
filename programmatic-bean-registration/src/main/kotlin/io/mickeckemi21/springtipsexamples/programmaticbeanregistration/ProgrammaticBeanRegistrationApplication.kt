package io.mickeckemi21.springtipsexamples.programmaticbeanregistration

import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import org.springframework.stereotype.Component
import java.util.function.Supplier

//@ComponentScan
//@Configuration
@SpringBootApplication
class ProgrammaticBeanRegistrationApplication {

    // Registering using ApplicationContextInitializer
//    @Component
    class MyBeanDefinitionRegistryPostProcessor : BeanDefinitionRegistryPostProcessor {
        override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        }

        override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
            registry.apply {
                registerBeanDefinition(
                    "barService",
                    genericBeanDefinition(BarService::class.java).beanDefinition
                )
                registerBeanDefinition(
                    "fooService",
                    genericBeanDefinition(FooService::class.java) {
                        val beanFactory = registry as BeanFactory
                        val barService = beanFactory.getBean(BarService::class.java)
                        FooService(barService)
                    }.beanDefinition
                )
            }
        }
    }

    // Registering using ComponentScan instead
//    @Bean
//    fun fooService(barService: BarService): FooService = FooService(barService)

    // Registering using BeanDefinitionRegistryPostProcessor
//    @Bean
//    fun barService(): BarService = BarService()

}

class ProgrammaticBeanDefinitionInitializr : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) {
        // Directly instantiating
//        val barService = BarService()
//        val fooService = FooService(barService)

        applicationContext.apply {
            registerBean(
                BarService::class.java,
                // OR supplying the directly instantiated object
                *emptyArray<Any>()
            )
            registerBean(
                FooService::class.java,
                Supplier {
                    // OR supplying the directly instantiated object
                    FooService(applicationContext.getBean(BarService::class.java))
                },
                *emptyArray<BeanDefinitionCustomizer>()
            )
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ProgrammaticBeanRegistrationApplication>(*args)
}

// Registering using BeanDefinitionRegistryPostProcessor
//@Component
class FooService(private val barService: BarService)

class BarService
