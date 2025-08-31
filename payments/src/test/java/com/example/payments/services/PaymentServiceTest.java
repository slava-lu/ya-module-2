package com.example.payments.services;

import com.example.payments.config.PaymentAccountProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

class PaymentServiceTest {

    private PaymentService paymentService;
    private static final String TEST_USERNAME = "test-user";
    private static final String ANOTHER_USERNAME = "another-user";

    @BeforeEach
    void setUp() {
        PaymentAccountProperties props = new PaymentAccountProperties();
        props.setInitialBalance(new BigDecimal("100.00"));
        paymentService = new PaymentService(props);
    }

    @Test
    void getBalance_returnsInitialBalanceForNewUser() {
        StepVerifier.create(paymentService.getBalance(TEST_USERNAME))
                .expectNext(new BigDecimal("100.00"))
                .verifyComplete();
    }

    @Test
    void processPayment_successful() {
        StepVerifier.create(paymentService.processPayment(TEST_USERNAME, new BigDecimal("40.00")))
                .expectNext("Payment successful! Remaining balance: 60.00")
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(TEST_USERNAME))
                .expectNext(new BigDecimal("60.00"))
                .verifyComplete();
    }

    @Test
    void processPayment_insufficientFunds() {
        StepVerifier.create(paymentService.processPayment(TEST_USERNAME, new BigDecimal("150.00")))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Insufficient funds"))
                .verify();

        StepVerifier.create(paymentService.getBalance(TEST_USERNAME))
                .expectNext(new BigDecimal("100.00"))
                .verifyComplete();
    }

    @Test
    void processPayment_handlesMultipleUsersIndependently() {
        StepVerifier.create(paymentService.processPayment(TEST_USERNAME, new BigDecimal("20.00")))
                .expectNext("Payment successful! Remaining balance: 80.00")
                .verifyComplete();

        StepVerifier.create(paymentService.processPayment(ANOTHER_USERNAME, new BigDecimal("50.00")))
                .expectNext("Payment successful! Remaining balance: 50.00")
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(TEST_USERNAME))
                .expectNext(new BigDecimal("80.00"))
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance(ANOTHER_USERNAME))
                .expectNext(new BigDecimal("50.00"))
                .verifyComplete();
    }

    @Test
    void processPayment_withZeroAmount() {
        StepVerifier.create(paymentService.processPayment(TEST_USERNAME, BigDecimal.ZERO))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Payment amount must be positive."))
                .verify();
    }

    @Test
    void processPayment_withNegativeAmount() {
        StepVerifier.create(paymentService.processPayment(TEST_USERNAME, new BigDecimal("-10.00")))
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                                throwable.getMessage().equals("Payment amount must be positive."))
                .verify();
    }
}
