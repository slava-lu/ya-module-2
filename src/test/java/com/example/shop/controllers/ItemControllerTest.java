package com.example.shop.controllers;

import com.example.shop.models.*;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@WebFluxTest(controllers = ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    private Page<Item> page;
    private Cart cart;

    @BeforeEach
    void setUp() {
        // prepare two items
        Item it1 = new Item(1L, "A", "desc A", "/img/a.png", BigDecimal.valueOf(5), 0);
        Item it2 = new Item(2L, "B", "desc B", "/img/b.png", BigDecimal.valueOf(10), 0);
        List<Item> content = List.of(it1, it2);
        page = new PageImpl<>(content, PageRequest.of(0, 10), content.size());

        // cart contains 3 of item1
        CartItem ci = new CartItem(); ci.setItem(it1); ci.setCount(3);
        cart = new Cart(); cart.setId(77L); cart.setItems(List.of(ci));
    }

    @Test
    void whenGetRoot_thenRedirectToMainItems() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/main/items");
    }

    @Test
    void whenShowItems_thenPagedAndCountsRendered() {
        when(itemService.getItems("", ItemSort.NO, 1, 10)).thenReturn(Mono.just(page));
        when(cartService.getOrCreateCart()).thenReturn(Mono.just(cart));

        webTestClient.get().uri(uriBuilder ->
                        uriBuilder.path("/main/items")
                                .queryParam("search", "")
                                .queryParam("sort", "NO")
                                .queryParam("pageSize", "10")
                                .queryParam("pageNumber", "1")
                                .build())
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(resp -> {
                    String html = resp.getResponseBody();
                    assert html.contains("A");
                    assert html.contains("B");
                    // item1 count = 3, item2 count = 0
                    assert html.contains(">3<");
                });

        verify(itemService).getItems("", ItemSort.NO, 1, 10);
        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenPostUpdateCount_plus_thenAddAndRedirect() {
        when(cartService.add(5L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/main/items/{id}")
                                .queryParam("action", "plus")
                                .queryParam("search", "foo")
                                .queryParam("sort", "ALPHA")
                                .queryParam("pageSize", "5")
                                .queryParam("pageNumber", "2")
                                .build(5L))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/main/items?search=foo&sort=ALPHA&pageSize=5&pageNumber=2");

        verify(cartService).add(5L);
    }

    @Test
    void whenPostUpdateCount_minus_thenRemoveAndRedirectDefaults() {
        when(cartService.remove(7L)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/main/items/7?action=minus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/main/items?search=&sort=NO&pageSize=10&pageNumber=1");

        verify(cartService).remove(7L);
    }

    @Test
    void whenGetShowItem_thenModelHasItemWithCount() {
        Item item = new Item(3L, "X", "desc X", "/img/x.png", BigDecimal.valueOf(20), 0);
        when(itemService.getById(3L)).thenReturn(Mono.just(item));
        CartItem ci = new CartItem(); ci.setItem(item); ci.setCount(4);
        Cart single = new Cart(); single.setId(88L); single.setItems(List.of(ci));
        when(cartService.getOrCreateCart()).thenReturn(Mono.just(single));

        webTestClient.get().uri("/items/3")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(resp -> {
                    String html = resp.getResponseBody();
                    assert html.contains("X");
                    assert html.contains(">4<");
                });

        verify(itemService).getById(3L);
        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenPostUpdateItemCount_plus_thenAddAndRedirectToDetail() {
        when(cartService.add(9L)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items/9?action=plus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items/9");

        verify(cartService).add(9L);
    }

    @Test
    void whenPostUpdateItemCount_minus_thenRemoveAndRedirectToDetail() {
        when(cartService.remove(10L)).thenReturn(Mono.empty());

        webTestClient.post().uri("/items/10?action=minus")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/items/10");

        verify(cartService).remove(10L);
    }
}
