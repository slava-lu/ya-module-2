package com.example.shop.config;

import com.example.payments.client.api.PaymentsApi;
import com.example.payments.client.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentClientConfig {

    @Bean
    public PaymentsApi paymentsApi(@Value("${payment.service.base-url}") String baseUrl) {
        ApiClient client = new ApiClient();
        client.setBasePath(baseUrl);
        return new PaymentsApi(client);
    }
}
