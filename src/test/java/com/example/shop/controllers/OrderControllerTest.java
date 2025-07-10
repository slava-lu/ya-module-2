package com.example.shop.controllers;

import com.example.shop.models.Order;
import com.example.shop.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void whenPostBuy_thenCreatesOrderAndRedirectsWithNewOrderParam() throws Exception {
        Order created = new Order();
        created.setId(123L);
        when(orderService.buyCart()).thenReturn(created);

        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/123?newOrder=true"));

        verify(orderService).buyCart();
    }


    @Test
    void whenShowOrder_defaultNewOrderFalse_thenModelHasOrderAndFlagFalse() throws Exception {
        Order order = new Order(); order.setId(10L);
        when(orderService.getById(10L)).thenReturn(order);

        mockMvc.perform(get("/orders/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", false));

        verify(orderService).getById(10L);
    }

    @Test
    void whenShowOrder_withNewOrderTrueParam_thenFlagTrue() throws Exception {
        Order order = new Order(); order.setId(20L);
        when(orderService.getById(20L)).thenReturn(order);

        mockMvc.perform(get("/orders/20")
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("newOrder", true));

        verify(orderService).getById(20L);
    }
}
