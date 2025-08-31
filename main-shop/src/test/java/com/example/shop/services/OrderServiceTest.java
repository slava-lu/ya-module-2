package com.example.shop.services;

import com.example.shop.models.*;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.OrderItemRepository;
import com.example.shop.repositories.OrderRepository;
import com.example.shop.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;
    @Mock
    private OrderItemRepository orderItemRepo;
    @Mock
    private CartService cartService;
    @Mock
    private CartItemRepository cartItemRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private PaymentServiceClient paymentServiceClient;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Cart cart;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        Item item = new Item(101L, "Test Item", "Desc", "/img", BigDecimal.TEN, 0);
        CartItem cartItem = new CartItem();
        cartItem.setItem(item);
        cartItem.setItemId(item.getId());
        cartItem.setCount(2);

        cart = new Cart();
        cart.setId(99L);
        cart.setUserId(user.getId());
        cart.setItems(List.of(cartItem));
    }

    @Test
    void buyCart_paymentDeclined_throwsException() {
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(cartService.getOrCreateCart(userDetails)).thenReturn(Mono.just(cart));
        when(paymentServiceClient.pay(any(BigDecimal.class))).thenReturn(Mono.error(new RuntimeException("Insufficient Funds")));

        StepVerifier.create(orderService.buyCart(userDetails))
                .expectError(IllegalStateException.class)
                .verify();

        verify(orderRepo, never()).save(any(Order.class));
    }

    @Test
    void findAllForUser_returnsUserOrders() {
        Order o1 = new Order();
        o1.setId(1L);
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(orderRepo.findByUserId(user.getId())).thenReturn(Flux.just(o1));

        StepVerifier.create(orderService.findAllForUser(userDetails))
                .expectNext(o1)
                .verifyComplete();
    }

    @Test
    void findByIdForUser_whenOwner_returnsOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(user.getId());
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(orderRepo.findById(10L)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.findByIdForUser(10L, userDetails))
                .expectNext(order)
                .verifyComplete();
    }

    @Test
    void findByIdForUser_whenNotOwner_throwsException() {
        Order order = new Order();
        order.setId(10L);
        order.setUserId(999L); // Different user
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(orderRepo.findById(10L)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.findByIdForUser(10L, userDetails))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}
