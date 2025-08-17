package com.example.payments.controllers;

import com.example.payments.api.model.PaymentRequest;
import com.example.payments.api.model.PaymentResponse;
import com.example.payments.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getBalance_returnsOkWithAmount() {
        BigDecimal balance = new BigDecimal("123.45");
        when(paymentService.getBalance()).thenReturn(Mono.just(balance));

        webTestClient.get()
                .uri("/payments/balance")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(BigDecimal.class)
                .isEqualTo(balance);
    }

    @Test
    void processPayment_returnsOkWithMessage() {
        when(paymentService.processPayment(eq(new BigDecimal("10"))))
                .thenReturn(Mono.just("PAID"));

        PaymentRequest body = new PaymentRequest().amount(new BigDecimal("10"));

        webTestClient.post()
                .uri("/payments/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(PaymentResponse.class)
                .value(resp -> assertEquals("PAID", resp.getMessage()));
    }
}
