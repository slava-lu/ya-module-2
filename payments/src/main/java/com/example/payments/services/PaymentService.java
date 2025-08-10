package com.example.payments.services;

import com.example.payments.config.PaymentAccountProperties;
import com.example.payments.models.PaymentAccount;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private final PaymentAccount account;

    public PaymentService(PaymentAccountProperties properties) {
        this.account = new PaymentAccount(properties.getId(), properties.getInitialBalance());
    }

    public Mono<BigDecimal> getBalance() {
        return Mono.just(account.getBalance());
    }

    public Mono<String> processPayment(BigDecimal amount) {
        if (amount.compareTo(account.getBalance()) <= 0) {
            account.setBalance(account.getBalance().subtract(amount));
            return Mono.just("Payment successful! Remaining balance: " + account.getBalance());
        } else {
            return Mono.error(new IllegalArgumentException("Insufficient funds"));
        }
    }
}