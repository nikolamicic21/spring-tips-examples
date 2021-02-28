package io.mickeckemi21.springtipsexamples.springsecurityoauthauthservice

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import java.util.*
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@SpringBootApplication
class SpringSecurityOauthAuthServiceApplication {

    @Bean
    fun cmdRunner(accountRepository: AccountRepository): CommandLineRunner = CommandLineRunner {
        listOf("jlong,spring", "dsyer,cloud", "pwebb,boot", "rwinch,security")
            .map { it.split(",") }
            .forEach { accountRepository.save(Account(it[0], it[1], true)) }
    }

}

fun main(args: Array<String>) {
    runApplication<SpringSecurityOauthAuthServiceApplication>(*args)
}

@Configuration
@EnableAuthorizationServer
class AuthorizationServiceConfig(
    private val authenticationManager: AuthenticationManager
) : AuthorizationServerConfigurerAdapter() {

    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.inMemory()
            .withClient("html5")
            .secret("password")
            .authorizedGrantTypes("password", "authorization_code")
            .scopes("openid")
    }

    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        endpoints.authenticationManager(authenticationManager)
    }
}

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = NoOpPasswordEncoder.getInstance()

}

@Service
class AccountUserDetailsService(
    private val accountRepository: AccountRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails =
        accountRepository.findByUsername(username)
            .map {
                User(
                    it.username,
                    it.password,
                    it.active,
                    it.active,
                    it.active,
                    it.active,
                    AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_USER")
                )
            }.orElseThrow { UsernameNotFoundException("couldn't find the username $username") }

}

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByUsername(username: String): Optional<Account>
}

@Entity
data class Account(
    val username: String,
    val password: String,
    val active: Boolean,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
