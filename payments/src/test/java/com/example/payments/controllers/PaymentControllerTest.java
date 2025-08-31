package com.example.payments.controllers;

import com.example.payments.api.model.PaymentRequest;
import com.example.payments.api.model.PaymentResponse;
import com.example.payments.config.SecurityConfig;
import com.example.payments.services.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void getBalance_returnsOkWithAmount() {
        String clientId = "main-shop-client";
        BigDecimal balance = new BigDecimal("123.45");

        when(paymentService.getBalance(eq(clientId))).thenReturn(Mono.just(balance));

        webTestClient
                .mutateWith(mockJwt().jwt(jwt -> jwt.subject(clientId).claim("scope", "payments.read")))
                .get()
                .uri("/payments/balance")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody(BigDecimal.class)
                .isEqualTo(balance);
    }

    @Test
    void getBalance_isForbiddenWithoutProperScope() {
        webTestClient
                .mutateWith(mockJwt())
                .get()
                .uri("/payments/balance")
                .exchange()
                .expectStatus().isForbidden();
    }
}