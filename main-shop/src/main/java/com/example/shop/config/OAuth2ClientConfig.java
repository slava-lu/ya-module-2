package com.example.shop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository
            (@Value("${spring.security.oauth2.client.provider.auth-server.token-uri}") String tokenUri) {
        ClientRegistration reg = ClientRegistration.withRegistrationId("main-shop-client")
                .clientId("main-shop-client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri(tokenUri)
                .scope("payments.read", "payments.write")
                .build();
        return new InMemoryReactiveClientRegistrationRepository(reg);
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientService reactiveAuthorizedClientService(
            ReactiveClientRegistrationRepository registrations
    ) {
        return new InMemoryReactiveOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager reactiveAuthorizedClientManager(
            ReactiveClientRegistrationRepository registrations,
            ReactiveOAuth2AuthorizedClientService clientService
    ) {
        var provider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(registrations, clientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    public WebClient oauth2WebClient(ReactiveOAuth2AuthorizedClientManager manager) {
        var oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("main-shop-client");

        return WebClient.builder()
                .filter(oauth2)
                .filter((request, next) -> {
                    String auth = request.headers().getFirst("Authorization");
                    System.out.println(">>> Outgoing Authorization: " +
                            (auth == null ? "null" : "Bearer " + auth.substring(7, Math.min(auth.length(), 27)) + "..."));
                    return next.exchange(request);
                })
                .build();
    }
}
