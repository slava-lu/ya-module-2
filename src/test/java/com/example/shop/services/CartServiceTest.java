package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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

    @InjectMocks
    private CartService service;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getOrCreateCart_firstTime_createsAndCachesId() {
        Cart saved = new Cart();
        saved.setId(100L);
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(saved));

        Cart result = service.getOrCreateCart().block();

        assertSame(saved, result);
        verify(cartRepo, never()).findById(anyLong());
        verify(cartRepo).save(any(Cart.class));
    }

    @Test
    void getOrCreateCart_subsequent_loadsExisting() {
        Cart firstSaved = new Cart();
        firstSaved.setId(200L);
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(firstSaved));

        Cart loaded = new Cart();
        loaded.setId(200L);
        when(cartRepo.findById(200L)).thenReturn(Mono.just(loaded));

        // first call creates
        Cart r1 = service.getOrCreateCart().block();
        assertSame(firstSaved, r1);

        // second call loads
        Cart r2 = service.getOrCreateCart().block();
        assertSame(loaded, r2);

        verify(cartRepo).save(any(Cart.class));
        verify(cartRepo).findById(200L);
    }

    @Test
    void getOrCreateCart_afterDelete_throws() {
        Cart first = new Cart();
        first.setId(300L);
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(first));
        when(cartRepo.findById(300L)).thenReturn(Mono.empty());

        service.getOrCreateCart().block();  // creates and caches

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.getOrCreateCart().block()
        );
        assertEquals("Demo cart was deleted", ex.getMessage());
    }

    @Test
    void add_newItem_createsCartItemWithCountOne() {
        CartService spySvc = Mockito.spy(service);

        Cart cart = new Cart();
        doReturn(Mono.just(cart)).when(spySvc).getOrCreateCart();

        Item item = new Item();
        item.setId(10L);
        item.setPrice(BigDecimal.valueOf(2.5));
        when(itemRepo.findById(10L)).thenReturn(Mono.just(item));

        Cart result = spySvc.add(10L).block();

        // result is the loaded cart
        assertSame(cart, result);
        assertEquals(1, cart.getItems().size());

        CartItem ci = cart.getItems().get(0);
        assertEquals(item, ci.getItem());
        assertEquals(1, ci.getCount());
    }

    @Test
    void add_existingItem_incrementsCount() {
        CartService spySvc = Mockito.spy(service);

        Cart cart = new Cart();
        Item item = new Item();
        item.setId(20L);
        CartItem existing = new CartItem();
        existing.setItemId(20L);
        existing.setItem(item);
        existing.setCount(5);
        existing.setCartId(1L);
        existing.setCart(cart);
        cart.getItems().add(existing);

        doReturn(Mono.just(cart)).when(spySvc).getOrCreateCart();
        when(itemRepo.findById(20L)).thenReturn(Mono.just(item));

        Cart result = spySvc.add(20L).block();

        assertSame(cart, result);
        assertEquals(1, cart.getItems().size());
        assertEquals(6, cart.getItems().get(0).getCount());
    }

    @Test
    void remove_countGreaterThanOne_decrementsOnly() {
        CartService spySvc = Mockito.spy(service);

        Cart cart = new Cart();
        Item item = new Item();
        item.setId(30L);
        CartItem ci = new CartItem();
        ci.setItemId(30L);
        ci.setItem(item);
        ci.setCount(3);
        ci.setCart(cart);
        cart.getItems().add(ci);

        doReturn(Mono.just(cart)).when(spySvc).getOrCreateCart();

        Cart result = spySvc.remove(30L).block();

        assertSame(cart, result);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getCount());
        verify(cartItemRepo, never()).delete(any());
    }

    @Test
    void remove_countEqualsOne_removesAndDeletes() {
        CartService spySvc = Mockito.spy(service);

        Cart cart = new Cart();
        Item item = new Item();
        item.setId(40L);
        CartItem ci = new CartItem();
        ci.setItemId(40L);
        ci.setItem(item);
        ci.setCount(1);
        ci.setCart(cart);
        cart.getItems().add(ci);

        doReturn(Mono.just(cart)).when(spySvc).getOrCreateCart();

        Cart result = spySvc.remove(40L).block();

        assertSame(cart, result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepo).delete(ci);
    }

    @Test
    void delete_alwaysRemovesAndDeletes() {
        CartService spySvc = Mockito.spy(service);

        Cart cart = new Cart();
        Item item = new Item();
        item.setId(50L);
        CartItem ci = new CartItem();
        ci.setItemId(50L);
        ci.setItem(item);
        ci.setCount(7);
        ci.setCart(cart);
        cart.getItems().add(ci);

        doReturn(Mono.just(cart)).when(spySvc).getOrCreateCart();

        Cart result = spySvc.delete(50L).block();

        assertSame(cart, result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartItemRepo).delete(ci);
    }
}
