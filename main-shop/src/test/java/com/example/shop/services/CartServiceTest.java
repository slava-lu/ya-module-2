package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepo;

    @Mock
    private CartItemRepository cartItemRepo;

    @Mock
    private ItemRepository itemRepo;

    @Mock
    private PaymentServiceClient paymentClient;


    private CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(cartRepo, cartItemRepo, itemRepo, paymentClient);
    }

    @Test
    void getOrCreateCart_firstTime_createsAndLoads() {
        Cart saved = new Cart();
        saved.setId(100L);
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(saved));
        when(cartRepo.findById(100L)).thenReturn(Mono.just(saved));
        when(cartItemRepo.findByCartId(100L)).thenReturn(Flux.empty());

        Cart result = service.getOrCreateCart().block();
        assertSame(saved, result);
        verify(cartRepo).save(any(Cart.class));
        verify(cartRepo).findById(100L);
        verifyNoMoreInteractions(cartRepo);
    }

    @Test
    void getOrCreateCart_subsequent_loadsExisting() {
        Cart firstSaved = new Cart();
        firstSaved.setId(200L);
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(firstSaved));

        Cart loaded = new Cart();
        loaded.setId(200L);
        when(cartRepo.findById(200L)).thenReturn(Mono.just(loaded));
        when(cartItemRepo.findByCartId(200L)).thenReturn(Flux.empty());

        Cart r1 = service.getOrCreateCart().block();
        assertSame(loaded, r1);

        Cart r2 = service.getOrCreateCart().block();
        assertSame(loaded, r2);

        verify(cartRepo).save(any(Cart.class));
        verify(cartRepo, times(2)).findById(200L);
    }

    @Test
    void getOrCreateCart_afterCartDeleted_throws() {
        Cart first = new Cart();
        first.setId(300L);
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(first));
        when(cartRepo.findById(300L))
                .thenReturn(Mono.just(first))
                .thenReturn(Mono.empty());
        when(cartItemRepo.findByCartId(300L)).thenReturn(Flux.empty());

        assertNotNull(service.getOrCreateCart().block());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.getOrCreateCart().block()
        );
        assertEquals("Cart not found: 300", ex.getMessage());
    }
}
