package com.example.shop.controllers;

import com.example.shop.config.SecurityConfig;
import com.example.shop.services.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = CartController.class)
@Import(SecurityConfig.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    @Test
    void showCart_withUnauthenticatedUser_redirectsToLogin() {
        webTestClient
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void showCart_withAuthenticatedUser_returnsCartView() {
        var cartPageData = new CartService.CartPageData(
                List.of(),
                new BigDecimal("50.00"),
                false,
                new BigDecimal("100.00"),
                false
        );
        when(cartService.buildCartPageData(any(UserDetails.class))).thenReturn(Mono.just(cartPageData));

        webTestClient
                .mutateWith(mockUser("user"))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .xpath("//*[contains(text(), '50.00')]").exists()
                .xpath("//*[contains(text(), '100.00')]").exists();

        verify(cartService).buildCartPageData(any(UserDetails.class));
    }

    @Test
    void whenPostUpdateCart_plus_thenAddAndRedirect() {
        when(cartService.add(eq(42L), any(UserDetails.class))).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser("user"))
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/cart/items/{id}")
                                .queryParam("action", "plus")
                                .build(42L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).add(eq(42L), any(UserDetails.class));
    }

    @Test
    void whenPostUpdateCart_minus_thenRemoveAndRedirect() {
        when(cartService.remove(eq(43L), any(UserDetails.class))).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser("user"))
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/cart/items/{id}")
                                .queryParam("action", "minus")
                                .build(43L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).remove(eq(43L), any(UserDetails.class));
    }

    @Test
    void whenPostUpdateCart_delete_thenDeleteAndRedirect() {
        when(cartService.delete(eq(44L), any(UserDetails.class))).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockUser("user"))
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/cart/items/{id}")
                                .queryParam("action", "delete")
                                .build(44L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).delete(eq(44L), any(UserDetails.class));
    }
}

