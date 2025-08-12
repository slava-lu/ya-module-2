package com.example.shop.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final WebClient paymentWebClient;

    public Mono<BigDecimal> getBalance() {
        return paymentWebClient.get()
                .uri("/payments/balance")
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(msg -> Mono.error(new IllegalStateException("Balance call failed: " + msg))))
                .bodyToMono(BigDecimal.class);
    }

    public Mono<String> pay(BigDecimal amount) {
        return paymentWebClient.post()
                .uri("/payments/pay")
                .bodyValue(new PaymentRequest(amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(PaymentResponse.class)
                                .defaultIfEmpty(new PaymentResponse("Payment failed"))
                                .flatMap(err -> Mono.error(new IllegalStateException(err.message())))
                )
                .bodyToMono(PaymentResponse.class)
                .map(PaymentResponse::message);
    }


    public record PaymentRequest(BigDecimal amount) {}
    public record PaymentResponse(String message) {}
}