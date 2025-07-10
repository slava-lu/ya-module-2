package com.example.shop.services;

import com.example.shop.models.*;
import com.example.shop.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        Item item1 = new Item(1L, "A", "d1", "/i1", BigDecimal.valueOf(2), 0);
        Item item2 = new Item(2L, "B", "d2", "/i2", BigDecimal.valueOf(3), 0);
        CartItem ci1 = new CartItem(11L, item1, null, 2);
        CartItem ci2 = new CartItem(12L, item2, null, 1);
        cart.getItems().addAll(Arrays.asList(ci1, ci2));
    }

    @Test
    void buyCart_createsOrderAndClearsCart() {
        when(cartService.getOrCreateCart()).thenReturn(cart);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        when(orderRepo.save(captor.capture())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(55L);
            return o;
        });

        Order result = orderService.buyCart();

        assertThat(result.getId()).isEqualTo(55L);
        List<OrderItem> items = result.getItems();
        assertThat(items).hasSize(2);
        OrderItem oi1 = items.get(0);
        OrderItem oi2 = items.get(1);
        assertThat(oi1.getItem().getId()).isEqualTo(1L);
        assertThat(oi1.getCount()).isEqualTo(2);
        assertThat(oi1.getOrder()).isSameAs(result);
        assertThat(oi2.getItem().getId()).isEqualTo(2L);
        assertThat(oi2.getCount()).isEqualTo(1);
        assertThat(cart.getItems()).isEmpty();
        verify(orderRepo).save(any(Order.class));
        verify(cartService).getOrCreateCart();
    }

    @Test
    void findAll_returnsAllOrders() {
        Order o1 = new Order(); o1.setId(1L);
        Order o2 = new Order(); o2.setId(2L);
        List<Order> list = Arrays.asList(o1, o2);
        when(orderRepo.findAll()).thenReturn(list);

        List<Order> result = orderService.findAll();

        assertThat(result).containsExactly(o1, o2);
        verify(orderRepo).findAll();
    }

    @Test
    void getById_existing_returnsOrder() {
        Order o = new Order(); o.setId(10L);
        when(orderRepo.findById(10L)).thenReturn(java.util.Optional.of(o));

        Order result = orderService.getById(10L);

        assertThat(result).isSameAs(o);
    }

    @Test
    void getById_missing_throwsException() {
        when(orderRepo.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> orderService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found: 99");
    }
}
