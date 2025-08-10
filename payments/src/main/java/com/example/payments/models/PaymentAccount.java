package com.example.payments.models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentAccount {
    private String accountId;
    private BigDecimal balance;

    // Constructor
    public PaymentAccount(String accountId, BigDecimal  balance) {
        this.accountId = accountId;
        this.balance = balance;
    }
}