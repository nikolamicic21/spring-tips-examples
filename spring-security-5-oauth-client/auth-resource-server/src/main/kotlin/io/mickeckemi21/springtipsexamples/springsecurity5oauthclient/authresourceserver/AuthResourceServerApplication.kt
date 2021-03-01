package io.mickeckemi21.springtipsexamples.springsecurity5oauthclient.authresourceserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.util.concurrent.ConcurrentHashMap

@SpringBootApplication
class AuthResourceServerApplication

fun main(args: Array<String>) {
    runApplication<AuthResourceServerApplication>(*args)
}

@Configuration
@EnableAuthorizationServer
class AuthorizationServerConfig(
    private val authenticationManager: AuthenticationManager,
) : AuthorizationServerConfigurerAdapter() {

    override fun configure(clients: ClientDetailsServiceConfigurer) {
        clients.inMemory()
            .withClient("client-1")
            .secret("client-1-secret")
            .authorizedGrantTypes("authorization_code")
            .scopes("profile")
            .redirectUris("http://localhost:8080/login/oauth2/code/login-client")
    }

    override fun configure(endpoints: AuthorizationServerEndpointsConfigurer) {
        endpoints.authenticationManager(authenticationManager)
            .tokenStore(tokenStore())
            .accessTokenConverter(tokenConverter())
    }

    @Bean
    fun tokenStore(): TokenStore = JwtTokenStore(tokenConverter())

    @Bean
    fun tokenConverter(): JwtAccessTokenConverter {
        val keyStoreFactory = KeyStoreKeyFactory(
            ClassPathResource(
                ".keystore-oauth2-demo"
            ),
            "admin1234".toCharArray()
        )
        return JwtAccessTokenConverter().apply {
            setKeyPair(keyStoreFactory.getKeyPair("oauth2-demo-key"))
        }
    }
}

@Service
class SimpleUserDetailsService : UserDetailsService {

    private val users = listOf("josh", "rob", "joe")
        .map { it to User(
            it,
            "pw",
            true,
            true,
            true,
            true,
            AuthorityUtils.createAuthorityList("USER")
        )
        }.toMap(ConcurrentHashMap())

    override fun loadUserByUsername(username: String): UserDetails =
        users[username]!!

}

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Bean
    override fun authenticationManagerBean(): AuthenticationManager =
        super.authenticationManagerBean()

    @Bean
    fun passwordEncoder(): PasswordEncoder = NoOpPasswordEncoder.getInstance()

}

@Configuration
@EnableResourceServer
class ResourceServerConfig : ResourceServerConfigurerAdapter() {

    override fun configure(http: HttpSecurity) {
        // @formatter:off
        http.antMatcher("/resources/**")
            .authorizeRequests()
            .mvcMatchers("/resources/userinfo")
                .access("#oauth2.hasScope('profile')")
        // @formatter:on
    }
}

@RestController
class ResourcesRestController {

    @GetMapping("/resources/userinfo")
    fun profile(principal: Principal): Map<String, String> =
        mapOf("name" to principal.name)

}
