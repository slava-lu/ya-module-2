package com.example.shop.controllers;

import com.example.shop.dtos.ItemCardDto;
import com.example.shop.dtos.ItemListDto;
import com.example.shop.dtos.SimplePage;
import com.example.shop.models.*;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    private SimplePage<ItemListDto> pageDto;
    private Cart cart;

    @BeforeEach
    void setUp() {
        ItemListDto it1 = new ItemListDto(1L, "A", "desc A", BigDecimal.valueOf(5), "/img/a.png");
        ItemListDto it2 = new ItemListDto(2L, "B", "desc B", BigDecimal.valueOf(10), "/img/b.png");

        pageDto = new SimplePage<>(
                List.of(it1, it2),
                1,
                10,
                2
        );

        Item full1 = new Item(1L, "A", "desc A", "/img/a.png", BigDecimal.valueOf(5), 0);
        CartItem ci = new CartItem();
        ci.setItemId(1L);
        ci.setItem(full1);
        ci.setCount(3);

        cart = new Cart();
        cart.setId(77L);
        cart.setItems(List.of(ci));
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
        when(itemService.getItemsPageSync(eq(""), eq(ItemSort.NO), eq(1), eq(10)))
                .thenReturn(pageDto);
        when(cartService.getOrCreateCart()).thenReturn(Mono.just(cart));

        webTestClient.get().uri(uriBuilder -> uriBuilder
                        .path("/main/items")
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
                    assert html != null;
                    assert html.contains("A");
                    assert html.contains("B");
                    assert html.contains(">3<");
                });

        verify(itemService).getItemsPageSync("", ItemSort.NO, 1, 10);
        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenGetShowItem_thenModelHasItemWithCount() {
        ItemCardDto dto = new ItemCardDto(3L, "/img/x.png", "X", "desc X", BigDecimal.valueOf(20));
        when(itemService.getItemCardSync(3L)).thenReturn(dto);

        Item full = new Item(3L, "X", "desc X", "/img/x.png", BigDecimal.valueOf(20), 0);
        CartItem ci = new CartItem();
        ci.setItemId(3L);
        ci.setItem(full);
        ci.setCount(4);

        Cart single = new Cart();
        single.setId(88L);
        single.setItems(List.of(ci));

        when(cartService.getOrCreateCart()).thenReturn(Mono.just(single));

        webTestClient.get().uri("/items/3")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(resp -> {
                    String html = resp.getResponseBody();
                    assert html != null;
                    assert html.contains("X");
                    assert html.contains(">4<");
                });

        verify(itemService).getItemCardSync(3L);
        verify(cartService).getOrCreateCart();
    }
}
