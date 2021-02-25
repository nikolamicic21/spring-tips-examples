package io.mickeckemi21.springtipsexamples.configuration.app

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertySource

@SpringBootApplication
@EnableConfigurationProperties(BootifulProperties::class)
class ConfigurationApplication {

    companion object {
        private val log = LoggerFactory.getLogger(ConfigurationApplication::class.java)
    }

    @Bean
    fun applicationRunner(
        env: Environment,
        @Value("\${greeting-message:\${message-from-application-yml}}") defaultValue: String,
        @Value("\${HOME}") userHome: String,
        @Value("\${message-from-program-args:}") messageFromProgramArgs: String,
        @Value("\${spring.datasource.url}") datasourceUrl: String,
        @Value("\${bootiful-message}") bootifulMessage: String,
        bootifulProperties: BootifulProperties,
        @Value("\${message-from-config-server}") messageFromConfigServer: String,
        @Value("\${message-from-vault-server}") messageFromVaultServer: String
    ): ApplicationRunner = ApplicationRunner {
        log.info("Message from application.yml: ${env.getProperty("message-from-application-yml")}")
        log.info("Message from foo.yml: ${env.getProperty("message-from-foo-yml")}")
        log.info("default value from application.yml: $defaultValue")
        log.info("user home from ENV_VAR: $userHome")
        log.info("message from PROGRAM_ARGS: $messageFromProgramArgs")
        log.info("spring.datasource.url: $datasourceUrl")
        log.info("message from custom property source: $bootifulMessage")
        log.info("message from BootifulProperties: ${bootifulProperties.message}")
        log.info("message from Spring Cloud Config Server: $messageFromConfigServer")
        log.info("message from Spring Cloud Vault Server: $messageFromVaultServer")
    }

//    @Autowired // 2.
//    fun contributeToPropertySources(configurableEnvironment: ConfigurableEnvironment) {
//        configurableEnvironment.propertySources.addLast(CustomBootifulPropertySource())
//    }

}

fun main(args: Array<String>) {
    System.setProperty("spring.config.name", "application,foo,bootstrap")
    System.setProperty("message-from-program-args", "Hello from Program Args!")
//    System.setProperty("spring.profiles.active", "dev")
//    runApplication<ConfigurationApplication>(*args)
    SpringApplicationBuilder()
        .sources(ConfigurationApplication::class.java)
//        .initializers(CustomBootifulPropertySourceContextInitializer()) // 1.
        .run(*args)
}

class CustomBootifulPropertySourceContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        applicationContext.environment.propertySources.addLast(CustomBootifulPropertySource())
    }

}

class CustomBootifulPropertySource : PropertySource<String>("bootiful") {

    override fun getProperty(name: String): Any? =
        if (name == "bootiful-message") "Hello from ${CustomBootifulPropertySource::class.java.simpleName}!"
        else null

}

@ConfigurationProperties(prefix = "bootiful")
@ConstructorBinding
data class BootifulProperties(
    val message: String
)
