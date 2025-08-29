package com.example.payments.controllers;

// No longer importing PaymentsApi
import com.example.payments.api.PaymentsApi;
import com.example.payments.api.model.PaymentRequest;
import com.example.payments.api.model.PaymentResponse;
import com.example.payments.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class PaymentController  {

    private final PaymentService paymentService;

    @GetMapping("/payments/balance")
    public Mono<ResponseEntity<BigDecimal>> getBalance(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String clientId = jwt.getSubject(); // usually your client-id
        return paymentService.getBalance(clientId).map(ResponseEntity::ok);
    }

    @PostMapping("/payments/pay")
    public Mono<ResponseEntity<PaymentResponse>> processPayment(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Mono<PaymentRequest> paymentRequest
    ) {
        String clientId = jwt.getSubject();
        return paymentRequest
                .flatMap(req -> paymentService.processPayment(clientId, req.getAmount()))
                .map(result -> ResponseEntity.ok(new PaymentResponse().message(result)));
    }
}