package com.example.shop.services;

import com.example.shop.models.*;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.OrderItemRepository;
import com.example.shop.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @Mock
    private OrderItemRepository orderItemRepo;

    @Mock
    private CartItemRepository cartItemRepo;

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
        CartItem ci1 = new CartItem(); ci1.setItemId(1L); ci1.setItem(item1); ci1.setCount(2);
        CartItem ci2 = new CartItem(); ci2.setItemId(2L); ci2.setItem(item2); ci2.setCount(1);
        cart.getItems().addAll(Arrays.asList(ci1, ci2));
        cart.setId(99L);
    }

    @Test
    void buyCart_createsOrderAndClearsCart() {

        when(cartService.getOrCreateCart()).thenReturn(Mono.just(cart));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepo.save(orderCaptor.capture()))
                .thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    o.setId(55L);
                    return Mono.just(o);
                });

        when(orderItemRepo.save(any(OrderItem.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(cartItemRepo.findByCartId(99L))
                .thenReturn(Flux.fromIterable(cart.getItems()));
        when(cartItemRepo.delete(any(CartItem.class)))
                .thenReturn(Mono.empty());


        Order result = orderService.buyCart().block();

        Order saved = orderCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(55L);
        List<OrderItem> savedItems = saved.getItems();
        assertThat(savedItems).hasSize(2);
        assertThat(saved.getTotal()).isEqualByComparingTo(
                BigDecimal.valueOf(2*2 + 3)
        );

        assertThat(result.getId()).isEqualTo(55L);

        assertThat(cart.getItems()).isEmpty();

        verify(orderRepo).save(any());
        verify(orderItemRepo, times(2)).save(any());
        verify(cartItemRepo).findByCartId(99L);
        verify(cartItemRepo, times(2)).delete(any());
        verify(cartService).getOrCreateCart();
    }

    @Test
    void findAll_returnsAllOrders() {
        Order o1 = new Order(); o1.setId(1L);
        Order o2 = new Order(); o2.setId(2L);
        when(orderRepo.findAll()).thenReturn(Flux.just(o1, o2));
        // stub loading each order (no items for simplicity)
        when(orderItemRepo.findByOrderId(anyLong())).thenReturn(Flux.empty());

        List<Order> result = orderService.findAll().collectList().block();

        assertThat(result).containsExactly(o1, o2);
        verify(orderRepo).findAll();
        verify(orderItemRepo, times(2)).findByOrderId(anyLong());
    }

    @Test
    void getById_existing_returnsOrder() {
        Order o = new Order(); o.setId(10L);
        when(orderRepo.findById(10L)).thenReturn(Mono.just(o));
        when(orderItemRepo.findByOrderId(10L)).thenReturn(Flux.empty());

        Order result = orderService.getById(10L).block();

        assertThat(result).isSameAs(o);
        verify(orderRepo).findById(10L);
        verify(orderItemRepo).findByOrderId(10L);
    }

    @Test
    void getById_missing_throwsException() {
        when(orderRepo.findById(99L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> orderService.getById(99L).block())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found: 99");
    }
}
