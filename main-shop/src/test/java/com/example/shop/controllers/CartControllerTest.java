package com.example.shop.controllers;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.services.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@WebFluxTest(controllers = CartController.class)
class CartControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CartService cartService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        Item item1 = new Item(1L, "Item1", "Desc1", "/img/1.png", BigDecimal.valueOf(2.5), 0);
        Item item2 = new Item(2L, "Item2", "Desc2", "/img/2.png", BigDecimal.valueOf(5.0), 0);
        CartItem ci1 = new CartItem(); ci1.setItem(item1); ci1.setCount(2);
        CartItem ci2 = new CartItem(); ci2.setItem(item2); ci2.setCount(1);
        cart = new Cart();
        cart.setId(99L);
        cart.setItems(List.of(ci1, ci2));

        Mockito.when(cartService.getOrCreateCart()).thenReturn(Mono.just(cart));
    }


    @Test
    void whenPostUpdateCart_plus_thenAddAndRedirect() {
        Mockito.when(cartService.add(42L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/cart/items/{id}")
                                .queryParam("action", "plus")
                                .build(42L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        Mockito.verify(cartService).add(42L);
    }

    @Test
    void whenPostUpdateCart_minus_thenRemoveAndRedirect() {
        Mockito.when(cartService.remove(43L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/cart/items/{id}")
                                .queryParam("action", "minus")
                                .build(43L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        Mockito.verify(cartService).remove(43L);
    }

    @Test
    void whenPostUpdateCart_delete_thenDeleteAndRedirect() {
        Mockito.when(cartService.delete(44L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/cart/items/{id}")
                                .queryParam("action", "delete")
                                .build(44L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        Mockito.verify(cartService).delete(44L);
    }
}
