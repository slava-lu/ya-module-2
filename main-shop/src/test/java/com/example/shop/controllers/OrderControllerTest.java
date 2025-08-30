package com.example.shop.controllers;

import com.example.shop.models.Order;
import com.example.shop.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@WebFluxTest(controllers = OrderController.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        order1 = new Order();
        order1.setId(10L);
        order2 = new Order();
        order2.setId(20L);
    }

    @Test
    void whenPostBuy_thenCreatesOrderAndRedirectsWithNewOrderParam() {
        Order created = new Order();
        created.setId(123L);
        when(orderService.buyCart()).thenReturn(Mono.just(created));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/123?newOrder=true");

        verify(orderService).buyCart();
    }

    @Test
    void whenListOrders_thenRendersOrdersPage() {
        when(orderService.findAll()).thenReturn(Flux.just(order1, order2));

        webTestClient.get()
                .uri("/orders")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(resp -> {
                    String html = resp.getResponseBody();
                    assert html.contains("10");
                    assert html.contains("20");
                });

        verify(orderService).findAll();
    }

    @Test
    void whenShowOrder_defaultNewOrderFalse_thenRendersOrderDetail() {
        when(orderService.getById(10L)).thenReturn(Mono.just(order1));

        webTestClient.get()
                .uri("/orders/10")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(resp -> {
                    String html = resp.getResponseBody();
                    assert html.contains("10");
                    assert !html.contains("newOrder");
                });

        verify(orderService).getById(10L);
    }

}
