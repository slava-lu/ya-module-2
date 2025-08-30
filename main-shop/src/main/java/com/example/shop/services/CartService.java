package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.models.User;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import com.example.shop.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ItemRepository itemRepo;
    private final UserRepository userRepo;
    private final PaymentServiceClient paymentClient;

    /** Resolve current user or empty if anonymous */
    private Mono<User> getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            return Mono.empty();
        }
        return userRepo.findByEmail(userDetails.getUsername());
    }

    /** A reusable empty cart for anonymous or when needed */
    private Cart createEmptyCart() {
        Cart empty = new Cart();
        empty.setItems(List.of());
        // leave id/userId unset (null); computed getters like getTotal()/isEmpty() should handle empty list
        return empty;
    }

    public Mono<Cart> getOrCreateCart(UserDetails userDetails) {
        // For anonymous users, return an empty cart (no NPE, no DB hits)
        return getCurrentUser(userDetails)
                .flatMap(user -> cartRepo.findByUserId(user.getId())
                        .switchIfEmpty(Mono.defer(() -> {
                            Cart newCart = new Cart();
                            newCart.setUserId(user.getId());
                            return cartRepo.save(newCart);
                        }))
                        .flatMap(cart -> loadCart(cart.getId()))
                )
                .switchIfEmpty(Mono.just(createEmptyCart()));
    }

    /** Guard to block any cart mutations for anonymous users */
    private <T> Mono<T> denyIfAnonymous(UserDetails userDetails) {
        if (userDetails == null) {
            return Mono.error(new AccessDeniedException("Login required to modify cart."));
        }
        return Mono.empty();
    }

    @Transactional
    public Mono<Cart> add(Long itemId, UserDetails userDetails) {
        return denyIfAnonymous(userDetails)
                .then(getOrCreateCart(userDetails))
                .flatMap(cart -> {
                    Long cid = cart.getId();
                    // If we somehow ended up with a cart without id (shouldn't happen for authenticated users), guard:
                    if (cid == null) {
                        return Mono.error(new IllegalStateException("Cart not persisted for authenticated user."));
                    }
                    return itemRepo.findById(itemId)
                            .switchIfEmpty(Mono.error(new IllegalArgumentException("No item " + itemId)))
                            .flatMap(item ->
                                    cartItemRepo.findByCartId(cid)
                                            .filter(ci -> ci.getItemId().equals(itemId))
                                            .next()
                                            .flatMap(existing -> {
                                                existing.setCount(existing.getCount() + 1);
                                                return cartItemRepo.save(existing);
                                            })
                                            .switchIfEmpty(Mono.defer(() -> {
                                                CartItem ci = new CartItem();
                                                ci.setCartId(cid);
                                                ci.setItemId(itemId);
                                                ci.setCount(1);
                                                return cartItemRepo.save(ci);
                                            }))
                            )
                            .thenReturn(cid);
                })
                .flatMap(this::loadCart);
    }

    @Transactional
    public Mono<Cart> remove(Long itemId, UserDetails userDetails) {
        return denyIfAnonymous(userDetails)
                .then(getOrCreateCart(userDetails))
                .flatMap(cart -> {
                    Long cid = cart.getId();
                    if (cid == null) {
                        return Mono.error(new IllegalStateException("Cart not persisted for authenticated user."));
                    }
                    return cartItemRepo.findByCartId(cid)
                            .filter(ci -> ci.getItemId().equals(itemId))
                            .next()
                            .flatMap(ci -> {
                                if (ci.getCount() > 1) {
                                    ci.setCount(ci.getCount() - 1);
                                    return cartItemRepo.save(ci).thenReturn(cid);
                                } else {
                                    return cartItemRepo.delete(ci).thenReturn(cid);
                                }
                            });
                })
                .flatMap(this::loadCart);
    }

    @Transactional
    public Mono<Cart> delete(Long itemId, UserDetails userDetails) {
        return denyIfAnonymous(userDetails)
                .then(getOrCreateCart(userDetails))
                .flatMap(cart -> {
                    Long cid = cart.getId();
                    if (cid == null) {
                        return Mono.error(new IllegalStateException("Cart not persisted for authenticated user."));
                    }
                    return cartItemRepo.findByCartId(cid)
                            .filter(ci -> ci.getItemId().equals(itemId))
                            .next()
                            .flatMap(ci -> cartItemRepo.delete(ci).thenReturn(cid));
                })
                .flatMap(this::loadCart);
    }

    private Mono<Cart> loadCart(Long cartId) {
        return cartRepo.findById(cartId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Cart not found: " + cartId)))
                .flatMap(cart ->
                        cartItemRepo.findByCartId(cartId)
                                .flatMap(ci ->
                                        itemRepo.findById(ci.getItemId())
                                                .doOnNext(ci::setItem)
                                                .thenReturn(ci)
                                )
                                .collectList()
                                .doOnNext(cart::setItems)
                                .thenReturn(cart)
                );
    }

    public Mono<CartPageData> buildCartPageData(UserDetails userDetails) {
        // Anonymous: show empty cart summary without touching payment service
        return getCurrentUser(userDetails)
                .flatMap(u ->
                        getOrCreateCart(userDetails)
                                .flatMap(cart ->
                                        paymentClient.getBalance().map(balance -> {
                                            List<Item> items = cart.getItems().stream()
                                                    .map(ci -> {
                                                        Item i = ci.getItem();
                                                        i.setCount(ci.getCount()); // count is @Transient on Item
                                                        return i;
                                                    })
                                                    .collect(Collectors.toList());

                                            BigDecimal total = cart.getTotal();
                                            boolean disableBuy = balance.compareTo(total) < 0;

                                            return new CartPageData(items, total, cart.isEmpty(), balance, disableBuy);
                                        })
                                )
                )
                .switchIfEmpty(Mono.just(new CartPageData(
                        List.of(),
                        BigDecimal.ZERO,
                        true,
                        BigDecimal.ZERO,
                        true
                )));
    }

    public record CartPageData(
            List<Item> items,
            BigDecimal total,
            boolean empty,
            BigDecimal balance,
            boolean disableBuy
    ) {}
}
