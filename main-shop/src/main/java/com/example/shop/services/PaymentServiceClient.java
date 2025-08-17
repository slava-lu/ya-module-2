package com.example.shop.services;

import com.example.payments.client.api.PaymentsApi;
import com.example.payments.client.model.PaymentRequest;
import com.example.payments.client.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final PaymentsApi paymentsApi;

    public Mono<BigDecimal> getBalance() {
        return paymentsApi.getBalance();
    }

    public Mono<String> pay(BigDecimal amount) {
        return paymentsApi
                .processPayment(new PaymentRequest().amount(amount))
                .map(PaymentResponse::getMessage);
    }
}
