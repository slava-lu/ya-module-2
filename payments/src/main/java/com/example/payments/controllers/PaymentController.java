package com.example.payments.controllers;

import com.example.payments.services.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/balance")
    public Mono<BigDecimal> getBalance() {
        return paymentService.getBalance();
    }

    @PostMapping("/pay")
    public Mono<ResponseEntity<PaymentResponse>> processPayment(@RequestBody PaymentRequest body) {
        return paymentService.processPayment(body.amount())
                .map(msg -> ResponseEntity.ok(new PaymentResponse(msg)))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                                .body(new PaymentResponse("Payment failed: " + e.getMessage()))));
    }

    // Inline DTOs
    public record PaymentRequest(BigDecimal amount) {}
    public record PaymentResponse(String message) {}
}