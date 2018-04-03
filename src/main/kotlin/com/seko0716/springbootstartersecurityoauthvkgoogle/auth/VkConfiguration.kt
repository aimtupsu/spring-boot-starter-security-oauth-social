package com.seko0716.springbootstartersecurityoauthvkgoogle.auth

import com.seko0716.springbootstartersecurityoauthvkgoogle.auth.extractors.VkPrincipalExtractor
import com.seko0716.springbootstartersecurityoauthvkgoogle.configurations.properties.VkProperties
import com.seko0716.springbootstartersecurityoauthvkgoogle.repository.IUserStorage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2ClientContext
import org.springframework.security.oauth2.client.OAuth2RestTemplate
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider
import java.util.*

@Configuration
@ComponentScan(basePackages = ["com.seko0716.springbootstartersecurityoauthvkgoogle"])
class VkConfiguration {
    @Autowired
    private lateinit var userStorage: IUserStorage
    @Autowired
    private lateinit var oauth2ClientContext: OAuth2ClientContext

    @Bean
    @ConditionalOnBean(name = ["vkClient", "vkResource", "authoritiesExtractor"])
    fun vkFilter(vkResource: ResourceServerProperties,
                 vkClient: AuthorizationCodeResourceDetails,
                 authoritiesExtractor: AuthoritiesExtractor): OAuth2ClientAuthenticationProcessingFilter {
        val vkFilter = OAuth2ClientAuthenticationProcessingFilter(vk().loginUrl)
        val vkTemplate = OAuth2RestTemplate(vkClient, oauth2ClientContext)
        vkFilter.setRestTemplate(vkTemplate)
        vkTemplate.setAccessTokenProvider(AccessTokenProviderChain(Arrays.asList(
                vkTokenProvider(), ImplicitAccessTokenProvider(),
                ResourceOwnerPasswordAccessTokenProvider(), ClientCredentialsAccessTokenProvider())))
        val tokenServices = UserInfoTokenServices(vkResource.userInfoUri, vkClient.clientId)
        tokenServices.setRestTemplate(vkTemplate)
        tokenServices.setTokenType("code")
        tokenServices.setAuthoritiesExtractor(authoritiesExtractor)
        tokenServices.setPrincipalExtractor(vkPrincipalExtractor())
        vkFilter.setTokenServices(tokenServices)
        return vkFilter
    }

    @Bean("vkClient")
    @ConfigurationProperties("vk.client")
    @ConditionalOnProperty(prefix = "vk.client", name = [
        "clientId", "clientSecret", "accessTokenUri", "userAuthorizationUri",
        "authenticationScheme", "clientAuthenticationScheme", "scope"
    ])
    fun vkClient(): AuthorizationCodeResourceDetails {
        return AuthorizationCodeResourceDetails()
    }

    @Bean("vkResource")
    @ConfigurationProperties("vk.resource")
    @ConditionalOnProperty(prefix = "vk.resource", name = ["userInfoUri"])
    fun vkResource(): ResourceServerProperties {
        return ResourceServerProperties()
    }

    @Bean("vk")
    @ConfigurationProperties("vk")
    fun vk(): VkProperties {
        return VkProperties()
    }

    @Bean
    fun vkPrincipalExtractor(): VkPrincipalExtractor {
        return VkPrincipalExtractor(userStorage = userStorage)
    }

    @Bean("vkTokenProvider")
    fun vkTokenProvider(): AuthorizationCodeAccessTokenProvider {
        return VkAuthorizationCodeAccessTokenProvider()
    }


}