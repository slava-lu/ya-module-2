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

    private Long demoCartId = null;

    public Mono<Cart> getOrCreateCart() {
        if (demoCartId != null) {
            return loadCart(demoCartId)
                    .switchIfEmpty(Mono.error(new IllegalStateException("Demo cart was deleted")));
        }
        return cartRepo.save(new Cart())
                .doOnNext(saved -> demoCartId = saved.getId())
                .flatMap(saved -> loadCart(saved.getId()));
    }

    public Mono<Cart> add(Long itemId) {
        return getOrCreateCart()
                .flatMap(cart ->
                        itemRepo.findById(itemId)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("No item " + itemId)))
                                .flatMap(item ->
                                        cartItemRepo.findByCartId(cart.getId())
                                                .filter(ci -> ci.getItemId().equals(itemId))
                                                .next()
                                                .flatMap(ci -> {
                                                    ci.setCount(ci.getCount() + 1);
                                                    return cartItemRepo.save(ci);
                                                })
                                                .switchIfEmpty(Mono.defer(() -> {
                                                    CartItem ci = new CartItem();
                                                    ci.setCartId(cart.getId());
                                                    ci.setItemId(itemId);
                                                    ci.setCount(1);
                                                    return cartItemRepo.save(ci);
                                                }))
                                )
                )
                .then(loadCart(demoCartId));
    }

    public Mono<Cart> remove(Long itemId) {
        return getOrCreateCart()
                .flatMap(cart ->
                        cartItemRepo.findByCartId(cart.getId())
                                .filter(ci -> ci.getItemId().equals(itemId))
                                .next()
                                .flatMap(ci -> {
                                    if (ci.getCount() > 1) {
                                        ci.setCount(ci.getCount() - 1);
                                        return cartItemRepo.save(ci);
                                    } else {
                                        return cartItemRepo.delete(ci).then();
                                    }
                                })
                )
                .then(loadCart(demoCartId));
    }

    public Mono<Cart> delete(Long itemId) {
        return getOrCreateCart()
                .flatMap(cart ->
                        cartItemRepo.findByCartId(cart.getId())
                                .filter(ci -> ci.getItemId().equals(itemId))
                                .next()
                                .flatMap(ci -> cartItemRepo.delete(ci))
                )
                .then(loadCart(demoCartId));
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
                                .doOnNext(list -> cart.setItems(list))
                                .thenReturn(cart)
                );
    }
}