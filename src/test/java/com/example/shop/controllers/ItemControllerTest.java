package com.example.shop.controllers;

import com.example.shop.models.*;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ItemService itemService;

    @Mock
    private CartService cartService;

    @InjectMocks
    private ItemController controller;

    @BeforeEach
    void setUp() {
        // Build MockMvc standalone so we don't start the full Spring context
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void whenGetRoot_thenRedirectToMainItems() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/items"));
    }

    @Test
    void whenShowItems_thenModelHasPagedItemsAndCounts() throws Exception {
        // prepare two items
        Item it1 = new Item(1L, "A", "desc A", "/img/a.png", BigDecimal.valueOf(5), 0);
        Item it2 = new Item(2L, "B", "desc B", "/img/b.png", BigDecimal.valueOf(10), 0);
        List<Item> content = List.of(it1, it2);
        Page<Item> page = new PageImpl<>(content, PageRequest.of(0, 10), content.size());

        when(itemService.getItems("", ItemSort.NO, 1, 10)).thenReturn(page);
        // cart contains 3 of item1
        CartItem ci = new CartItem(99L, it1, null, 3);
        Cart cart = new Cart(77L, List.of(ci));
        when(cartService.getOrCreateCart()).thenReturn(cart);

        var mvc = mockMvc.perform(get("/main/items"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("search", ""))
                .andExpect(model().attribute("sort", ItemSort.NO))
                .andExpect(model().attributeExists("items", "paging"))
                .andReturn();

        @SuppressWarnings("unchecked")
        var rows = (List<List<Item>>) mvc.getModelAndView().getModel().get("items");
        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).size());
        assertEquals(3, rows.get(0).get(0).getCount());
        assertEquals(0, rows.get(0).get(1).getCount());

        verify(itemService).getItems("", ItemSort.NO, 1, 10);
        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenPostUpdateCount_plus_thenCartServiceAddAndRedirect() throws Exception {
        mockMvc.perform(post("/main/items/5")
                        .param("action", "plus")
                        .param("search", "foo")
                        .param("sort", "ALPHA")
                        .param("pageSize", "5")
                        .param("pageNumber", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/items?search=foo&sort=ALPHA&pageSize=5&pageNumber=2"));

        verify(cartService).add(5L);
    }

    @Test
    void whenPostUpdateCount_minus_thenCartServiceRemoveAndRedirectDefaults() throws Exception {
        mockMvc.perform(post("/main/items/7")
                        .param("action", "minus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main/items?search=&sort=NO&pageSize=10&pageNumber=1"));

        verify(cartService).remove(7L);
    }

    @Test
    void whenGetShowItem_thenModelHasItemWithCount() throws Exception {
        Item item = new Item(3L, "X", "desc X", "/img/x.png", BigDecimal.valueOf(20), 0);
        when(itemService.getById(3L)).thenReturn(item);

        CartItem ci = new CartItem(55L, item, null, 4);
        Cart cart = new Cart(88L, List.of(ci));
        when(cartService.getOrCreateCart()).thenReturn(cart);

        var mvc = mockMvc.perform(get("/items/3"))
                .andExpect(status().isOk())
                .andExpect(view().name("item"))
                .andExpect(model().attributeExists("item"))
                .andReturn();

        Item modelItem = (Item) mvc.getModelAndView().getModel().get("item");
        assertEquals(4, modelItem.getCount());

        verify(itemService).getById(3L);
        verify(cartService).getOrCreateCart();
    }

    @Test
    void whenPostUpdateItemCount_plus_thenAddAndRedirectToDetail() throws Exception {
        mockMvc.perform(post("/items/9").param("action", "plus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/9"));

        verify(cartService).add(9L);
    }

    @Test
    void whenPostUpdateItemCount_minus_thenRemoveAndRedirectToDetail() throws Exception {
        mockMvc.perform(post("/items/10").param("action", "minus"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/10"));

        verify(cartService).remove(10L);
    }
}
