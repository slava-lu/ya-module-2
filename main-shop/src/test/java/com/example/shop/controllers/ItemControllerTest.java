package com.example.shop.controllers;

import com.example.shop.config.SecurityConfig;
import com.example.shop.dtos.ItemCardDto;
import com.example.shop.dtos.ItemListDto;
import com.example.shop.dtos.SimplePage;
import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
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
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

@WebFluxTest(controllers = ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private ReactiveUserDetailsService reactiveUserDetailsService;

    private SimplePage<ItemListDto> pageDto;
    private Cart cartWithItems;
    private Cart emptyCart;

    @BeforeEach
    void setUp() {
        ItemListDto it1 = new ItemListDto(1L, "A", "desc A", BigDecimal.valueOf(5), "/img/a.png");
        ItemListDto it2 = new ItemListDto(2L, "B", "desc B", BigDecimal.valueOf(10), "/img/b.png");

        pageDto = new SimplePage<>(List.of(it1, it2), 1, 10, 2);

        Item fullItem1 = new Item(1L, "A", "desc A", "/img/a.png", BigDecimal.valueOf(5), 0);
        CartItem cartItem = new CartItem();
        cartItem.setItemId(1L);
        cartItem.setItem(fullItem1);
        cartItem.setCount(3);

        cartWithItems = new Cart();
        cartWithItems.setId(77L);
        cartWithItems.setItems(List.of(cartItem));

        emptyCart = new Cart();
        emptyCart.setItems(List.of());
    }

    @Test
    void whenGetRoot_thenRedirectToMainItems() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().location("/main/items");
    }

    @Test
    void whenShowItems_withUnauthenticatedUser_thenCountsAreZero() {
        when(itemService.getItemsPageSync(any(), any(), any(Integer.class), any(Integer.class))).thenReturn(pageDto);
        when(cartService.getOrCreateCart(isNull())).thenReturn(Mono.just(emptyCart));

        webTestClient.get().uri("/main/items")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("A");
                    assertThat(html).contains("B");
                    assertThat(html).doesNotContain(">3<");
                });

        verify(cartService).getOrCreateCart(isNull());
    }

    @Test
    void whenShowItems_withAuthenticatedUser_thenPagedAndCountsRendered() {
        when(itemService.getItemsPageSync(eq(""), eq(ItemSort.NO), eq(1), eq(10))).thenReturn(pageDto);
        when(cartService.getOrCreateCart(any(UserDetails.class))).thenReturn(Mono.just(cartWithItems));

        webTestClient.mutateWith(mockUser("user"))
                .get().uri("/main/items")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("A");
                    assertThat(html).contains("B");
                    assertThat(html).contains(">3<");
                });

        verify(itemService).getItemsPageSync("", ItemSort.NO, 1, 10);
        verify(cartService).getOrCreateCart(any(UserDetails.class));
    }

    @Test
    void whenGetShowItem_withAuthenticatedUser_thenModelHasItemWithCount() {
        ItemCardDto dto = new ItemCardDto(1L, "/img/a.png", "A", "desc A", BigDecimal.valueOf(5));
        when(itemService.getItemCardSync(1L)).thenReturn(dto);
        when(cartService.getOrCreateCart(any(UserDetails.class))).thenReturn(Mono.just(cartWithItems));

        webTestClient.mutateWith(mockUser("user"))
                .get().uri("/items/1")
                .accept(MediaType.TEXT_HTML)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("A");
                    assertThat(html).contains(">3<");
                });

        verify(itemService).getItemCardSync(1L);
        verify(cartService).getOrCreateCart(any(UserDetails.class));
    }

    @Test
    void whenUpdateCount_withUnauthenticatedUser_redirectsToLogin() {
        webTestClient.post().uri("/main/items/1")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login");
    }

    @Test
    void whenUpdateCount_plus_callsServiceAndRedirects() {
        when(cartService.add(eq(1L), any(UserDetails.class))).thenReturn(Mono.empty());
        String expectedRedirectUrl = "/main/items?search=test&sort=NO&pageSize=5&pageNumber=2";

        webTestClient.mutateWith(mockUser("user"))
                .post()
                .uri(UriComponentsBuilder.fromPath("/main/items/1")
                        .queryParam("action", "plus")
                        .queryParam("search", "test")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "2")
                        .build().toUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", expectedRedirectUrl);

        verify(cartService).add(eq(1L), any(UserDetails.class));
    }

}

