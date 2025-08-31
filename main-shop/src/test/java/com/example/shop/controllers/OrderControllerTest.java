package com.example.shop.controllers;

import com.example.shop.config.SecurityConfig;
import com.example.shop.models.Order;
import com.example.shop.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ReactiveUserDetailsService reactiveUserDetailsService;

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
    void whenPostBuy_withUnauthenticatedUser_redirectsToLogin() {
        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void whenPostBuy_withAuthenticatedUser_createsOrderAndRedirects() {
        Order created = new Order();
        created.setId(123L);
        when(orderService.buyCart(any(UserDetails.class))).thenReturn(Mono.just(created));

        webTestClient.mutateWith(mockUser("user"))
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/123?newOrder=true");

        verify(orderService).buyCart(any(UserDetails.class));
    }

    @Test
    void whenListOrders_withUnauthenticatedUser_redirectsToLogin() {
        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void whenListOrders_withAuthenticatedUser_rendersOrdersPage() {
        when(orderService.findAllForUser(any(UserDetails.class))).thenReturn(Flux.just(order1, order2));

        webTestClient.mutateWith(mockUser("user"))
                .get().uri("/orders")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("10");
                    assertThat(html).contains("20");
                });

        verify(orderService).findAllForUser(any(UserDetails.class));
    }

    @Test
    void whenShowOrder_withUnauthenticatedUser_redirectsToLogin() {
        webTestClient.get().uri("/orders/10")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void whenShowOrder_withAuthenticatedUser_rendersOrderDetail() {
        when(orderService.findByIdForUser(eq(10L), any(UserDetails.class))).thenReturn(Mono.just(order1));

        webTestClient.mutateWith(mockUser("user"))
                .get().uri("/orders/10")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("10"));

        verify(orderService).findByIdForUser(eq(10L), any(UserDetails.class));
    }
}
