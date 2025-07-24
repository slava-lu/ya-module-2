package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ItemRepository itemRepo;

    /**
     * In-memory demo cart id: once we create one, we keep returning it.
     */
    private Long demoCartId = null;

    /**
     * Load an existing cart or create a new one (and remember its id in demoCartId).
     */
    public Mono<Cart> getOrCreateCart() {
        if (demoCartId != null) {
            return loadCart(demoCartId)
                    .switchIfEmpty(Mono.error(new IllegalStateException("Demo cart was deleted")));
        }
        return cartRepo.save(new Cart())
                .doOnNext(saved -> demoCartId = saved.getId())
                .flatMap(saved -> loadCart(saved.getId()));
    }

    /**
     * Add one of the given item to the cart.
     */
    public Mono<Cart> add(Long itemId) {
        return getOrCreateCart()
                // remember the cart id for the final reload
                .flatMap(cart -> {
                    Long cid = cart.getId();
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

    /**
     * Remove one of the given item from the cart (or delete the CartItem if it was the last one).
     */
    public Mono<Cart> remove(Long itemId) {
        return getOrCreateCart()
                .flatMap(cart -> {
                    Long cid = cart.getId();
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

    /**
     * Delete the CartItem entirely.
     */
    public Mono<Cart> delete(Long itemId) {
        return getOrCreateCart()
                .flatMap(cart -> {
                    Long cid = cart.getId();
                    return cartItemRepo.findByCartId(cid)
                            .filter(ci -> ci.getItemId().equals(itemId))
                            .next()
                            .flatMap(ci -> cartItemRepo.delete(ci).thenReturn(cid));
                })
                .flatMap(this::loadCart);
    }

    /**
     * Load the Cart + its Items by id (or error).
     */
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
                                .doOnNext(list -> cart.setItems(list))
                                .thenReturn(cart)
                );
    }
}
