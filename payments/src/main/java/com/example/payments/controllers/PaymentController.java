package com.example.payments.controllers;

import com.example.payments.api.PaymentsApi;
import com.example.payments.api.model.PaymentRequest;
import com.example.payments.api.model.PaymentResponse;
import com.example.payments.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;

    @Override
    public Mono<ResponseEntity<BigDecimal>> getBalance(ServerWebExchange exchange) {
        return paymentService.getBalance()
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> processPayment(
            Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange) {

        return paymentRequest
                .flatMap(req -> paymentService.processPayment(req.getAmount()))
                .map(result -> ResponseEntity.ok(new PaymentResponse().message(result)));
    }
}