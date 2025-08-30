package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.models.User;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import com.example.shop.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private UserRepository userRepo;
    @Mock
    private PaymentServiceClient paymentClient;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private CartService service;

    private User user;
    private Cart cart;
    private Item item1;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        cart = new Cart();
        cart.setId(10L);
        cart.setUserId(user.getId());

        item1 = new Item(101L, "Item 1", "Desc 1", "/img/1.png", BigDecimal.TEN, 0);
    }

    @Test
    void getOrCreateCart_forAnonymousUser_returnsEmptyNonPersistedCart() {
        StepVerifier.create(service.getOrCreateCart(null))
                .assertNext(result -> {
                    assertThat(result.getId()).isNull();
                    assertThat(result.getItems()).isEmpty();
                })
                .verifyComplete();

        verifyNoInteractions(userRepo, cartRepo);
    }

    @Test
    void getOrCreateCart_forNewUser_createsAndSavesCart() {
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(cartRepo.findByUserId(user.getId())).thenReturn(Mono.empty());
        when(cartRepo.save(any(Cart.class))).thenReturn(Mono.just(cart));
        when(cartRepo.findById(cart.getId())).thenReturn(Mono.just(cart));
        when(cartItemRepo.findByCartId(cart.getId())).thenReturn(Flux.empty());

        StepVerifier.create(service.getOrCreateCart(userDetails))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(cart.getId()))
                .verifyComplete();

        verify(cartRepo).save(argThat(c -> c.getUserId().equals(user.getId())));
    }

    @Test
    void getOrCreateCart_forExistingUser_loadsCart() {
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(cartRepo.findByUserId(user.getId())).thenReturn(Mono.just(cart));
        when(cartRepo.findById(cart.getId())).thenReturn(Mono.just(cart));
        when(cartItemRepo.findByCartId(cart.getId())).thenReturn(Flux.empty());

        StepVerifier.create(service.getOrCreateCart(userDetails))
                .assertNext(result -> assertThat(result.getId()).isEqualTo(cart.getId()))
                .verifyComplete();

        verify(cartRepo, never()).save(any(Cart.class));
    }

    @Test
    void add_forAnonymousUser_throwsAccessDenied() {
        StepVerifier.create(service.add(1L, null))
                .expectError(AccessDeniedException.class)
                .verify();
    }

    @Test
    void add_newItem_createsCartItem() {
        // Arrange for getOrCreateCart
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        when(cartRepo.findByUserId(user.getId())).thenReturn(Mono.just(cart));
        // Arrange for loadCart (called twice)
        when(cartRepo.findById(cart.getId())).thenReturn(Mono.just(cart));

        // Arrange for add logic
        when(itemRepo.findById(item1.getId())).thenReturn(Mono.just(item1));
        // Simulate no existing cart item for this item
        when(cartItemRepo.findByCartId(cart.getId())).thenReturn(Flux.empty());

        CartItem newCartItem = new CartItem();
        newCartItem.setCartId(cart.getId());
        newCartItem.setItemId(item1.getId());
        newCartItem.setCount(1);
        when(cartItemRepo.save(any(CartItem.class))).thenReturn(Mono.just(newCartItem));

        // Act & Assert
        StepVerifier.create(service.add(item1.getId(), userDetails))
                .assertNext(updatedCart -> assertThat(updatedCart.getId()).isEqualTo(cart.getId()))
                .verifyComplete();

        verify(cartItemRepo).save(argThat(ci ->
                ci.getItemId().equals(item1.getId()) && ci.getCount() == 1
        ));
    }

    @Test
    void buildCartPageData_forAuthenticatedUser_returnsCorrectData() {
        CartItem cartItem = new CartItem();
        cartItem.setItem(item1);
        cartItem.setCount(2);
        cart.setItems(List.of(cartItem));

        // Arrange getOrCreateCart
        when(userDetails.getUsername()).thenReturn(user.getEmail());
        when(userRepo.findByEmail(anyString())).thenReturn(Mono.just(user));
        when(cartRepo.findByUserId(anyLong())).thenReturn(Mono.just(cart));
        when(cartRepo.findById(anyLong())).thenReturn(Mono.just(cart));
        when(cartItemRepo.findByCartId(anyLong())).thenReturn(Flux.just(cartItem));
        when(itemRepo.findById(anyLong())).thenReturn(Mono.just(item1));

        // Arrange payment client
        when(paymentClient.getBalance()).thenReturn(Mono.just(new BigDecimal("100")));

        StepVerifier.create(service.buildCartPageData(userDetails))
                .assertNext(data -> {
                    assertThat(data.empty()).isFalse();
                    assertThat(data.total()).isEqualTo(new BigDecimal("20")); // 2 * 10
                    assertThat(data.balance()).isEqualTo(new BigDecimal("100"));
                    assertThat(data.disableBuy()).isFalse();
                    assertThat(data.items()).hasSize(1);
                    assertThat(data.items().get(0).getCount()).isEqualTo(2);
                })
                .verifyComplete();
    }
}
