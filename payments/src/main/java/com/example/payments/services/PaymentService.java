package com.example.payments.services;

import com.example.payments.config.PaymentAccountProperties;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    private final Map<String, BigDecimal> userBalances = new ConcurrentHashMap<>();
    private final BigDecimal initialBalance;

    public PaymentService(PaymentAccountProperties properties) {
        this.initialBalance = properties.getInitialBalance();
    }

    public Mono<BigDecimal> getBalance(String username) {
        BigDecimal balance = userBalances.computeIfAbsent(username, key -> initialBalance);
        return Mono.just(balance);
    }

    public Mono<String> processPayment(String username, BigDecimal amount) {
        BigDecimal currentBalance = userBalances.computeIfAbsent(username, key -> initialBalance);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new IllegalArgumentException("Payment amount must be positive."));
        }

        if (currentBalance.compareTo(amount) >= 0) {
            BigDecimal newBalance = currentBalance.subtract(amount);
            userBalances.put(username, newBalance);
            return Mono.just("Payment successful! Remaining balance: " + newBalance);
        } else {
            return Mono.error(new IllegalArgumentException("Insufficient funds"));
        }
    }
}