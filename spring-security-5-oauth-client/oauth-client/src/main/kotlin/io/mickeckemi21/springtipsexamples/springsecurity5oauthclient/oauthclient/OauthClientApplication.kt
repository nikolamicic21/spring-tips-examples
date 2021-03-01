package io.mickeckemi21.springtipsexamples.springsecurity5oauthclient.oauthclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.net.URI

@SpringBootApplication
class OauthClientApplication {

    @Bean
    fun restTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        oAuth2AuthorizedClientService: OAuth2AuthorizedClientService
    ): RestTemplate = restTemplateBuilder
        .interceptors(ClientHttpRequestInterceptor { request, body, execution ->
            val authenticationToken = SecurityContextHolder
                .getContext()
                .authentication as OAuth2AuthenticationToken
            val authorizedClient = oAuth2AuthorizedClientService
                .loadAuthorizedClient<OAuth2AuthorizedClient>(
                    authenticationToken.authorizedClientRegistrationId,
                    authenticationToken.name
                )
            request.headers.add(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${authorizedClient.accessToken.tokenValue}"
            )
            execution.execute(request, body)
        })
        .build()


}

fun main(args: Array<String>) {
    runApplication<OauthClientApplication>(*args)
}

@RestController
@RequestMapping("/profile")
class ProfileRestController(
    private val restTemplate: RestTemplate,
    private val oAuth2AuthorizedClientService: OAuth2AuthorizedClientService
) {

    @GetMapping
    fun profile(
        oAuth2AuthenticationToken: OAuth2AuthenticationToken
    ): PrincipalDetails {
        val authorizedClient = oAuth2AuthorizedClientService
            .loadAuthorizedClient<OAuth2AuthorizedClient>(
                oAuth2AuthenticationToken.authorizedClientRegistrationId,
                oAuth2AuthenticationToken.name
            )
        val userInfoUri = authorizedClient.clientRegistration
            .providerDetails
            .userInfoEndpoint
            .uri

        return restTemplate.exchange(
            URI(userInfoUri),
            HttpMethod.GET,
            null,
            PrincipalDetails::class.java
        ).body!!
    }

}

data class PrincipalDetails(
    val name: String
)
