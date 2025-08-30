package com.example.shop.config;

import com.example.payments.client.api.PaymentsApi;
import com.example.payments.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PaymentClientConfig {

    @Bean
    public PaymentsApi paymentsApi(
            @Value("${payment.service.base-url}") String baseUrl,
            WebClient oauth2WebClient
    ) {
        ApiClient apiClient = new ApiClient(oauth2WebClient);
        apiClient.setBasePath(baseUrl);
        return new PaymentsApi(apiClient);
    }
}