package com.example.payments.services;

import com.example.payments.config.PaymentAccountProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentAccountProperties props = new PaymentAccountProperties();
        props.setId("acc-123");
        props.setInitialBalance(new BigDecimal("100.00"));
        paymentService = new PaymentService(props);
    }

    @Test
    void processPayment_successful() {
        StepVerifier.create(paymentService.processPayment(new BigDecimal("40.00")))
                .expectNext("Payment successful! Remaining balance: 60.00")
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance())
                .expectNext(new BigDecimal("60.00"))
                .verifyComplete();
    }

    @Test
    void processPayment_insufficientFunds() {
        StepVerifier.create(paymentService.processPayment(new BigDecimal("150.00")))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Insufficient funds"))
                .verify();
    }
}
