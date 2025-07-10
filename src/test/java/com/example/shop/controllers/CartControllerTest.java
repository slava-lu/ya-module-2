package com.example.shop.controllers;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.services.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void whenShowCart_thenModelHasItemsTotalAndEmptyFlag() throws Exception {
        // Prepare two items in the cart
        Item item1 = new Item(1L, "Item1", "Desc1", "/img/1.png", BigDecimal.valueOf(2.5), 0);
        Item item2 = new Item(2L, "Item2", "Desc2", "/img/2.png", BigDecimal.valueOf(5.0), 0);
        CartItem ci1 = new CartItem(11L, item1, null, 2);
        CartItem ci2 = new CartItem(12L, item2, null, 1);
        Cart cart = new Cart(99L, List.of(ci1, ci2));
        when(cartService.getOrCreateCart()).thenReturn(cart);

        var mvc = mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items", "total", "empty"))
                .andReturn();

        @SuppressWarnings("unchecked")
        var items = (List<Item>) mvc.getModelAndView().getModel().get("items");
        BigDecimal total = (BigDecimal) mvc.getModelAndView().getModel().get("total");
        Boolean empty = (Boolean) mvc.getModelAndView().getModel().get("empty");

        // The controller should copy count from CartItem into each Item
        assertEquals(2, items.size());
        assertEquals(2, items.get(0).getCount());
        assertEquals(1, items.get(1).getCount());

        // Total = 2 * 2.5 + 1 * 5.0 = 10.0
        assertEquals(BigDecimal.valueOf(10.0), total);

        // Cart is not empty
        assertFalse(empty);

        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenShowCart_andEmptyCart_thenEmptyFlagTrue() throws Exception {
        Cart emptyCart = new Cart(100L, List.of());
        when(cartService.getOrCreateCart()).thenReturn(emptyCart);

        var mvc = mockMvc.perform(get("/cart/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("empty", true))
                .andReturn();

        @SuppressWarnings("unchecked")
        var items = (List<Item>) mvc.getModelAndView().getModel().get("items");
        BigDecimal total = (BigDecimal) mvc.getModelAndView().getModel().get("total");

        assertTrue(items.isEmpty());
        assertEquals(BigDecimal.ZERO, total);

        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenPostUpdateCart_plus_thenAddAndRedirect() throws Exception {
        mockMvc.perform(post("/cart/items/42")
                        .param("action", "plus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));
        verify(cartService).add(42L);
    }

    @Test
    void whenPostUpdateCart_minus_thenRemoveAndRedirect() throws Exception {
        mockMvc.perform(post("/cart/items/43")
                        .param("action", "minus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));
        verify(cartService).remove(43L);
    }

    @Test
    void whenPostUpdateCart_delete_thenDeleteAndRedirect() throws Exception {
        mockMvc.perform(post("/cart/items/44")
                        .param("action", "delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));
        verify(cartService).delete(44L);
    }
}
