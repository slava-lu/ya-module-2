package com.example.payments.controllers;

import com.example.payments.services.PaymentService;
import lombok.RequiredArgsConstructor;
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
    public Mono<String> processPayment(@RequestParam BigDecimal amount) {
        return paymentService.processPayment(amount)
                .onErrorResume(e -> Mono.just("Payment failed: " + e.getMessage()));
    }
}