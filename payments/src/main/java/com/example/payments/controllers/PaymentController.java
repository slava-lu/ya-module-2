package com.example.payments.controllers;

// No longer importing PaymentsApi
import com.example.payments.api.PaymentsApi;
import com.example.payments.api.model.PaymentRequest;
import com.example.payments.api.model.PaymentResponse;
import com.example.payments.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class PaymentController implements PaymentsApi {

    private final PaymentService paymentService;

    @GetMapping("/payments/balance")
    public Mono<ResponseEntity<BigDecimal>> getBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            ServerWebExchange exchange
    ) {
        return paymentService.getBalance(userDetails.getUsername())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/payments/pay")
    public Mono<ResponseEntity<PaymentResponse>> processPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {
        return paymentRequest
                .flatMap(req -> paymentService.processPayment(
                        userDetails.getUsername(),
                        req.getAmount()
                ))
                .map(result -> ResponseEntity.ok(new PaymentResponse().message(result)));
    }
}