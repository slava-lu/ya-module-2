package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    private final PaymentServiceClient paymentClient;

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

    public Mono<Cart> add(Long itemId) { return getOrCreateCart()
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

    public Mono<Cart> remove(Long itemId) { return getOrCreateCart()
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

    public Mono<Cart> delete(Long itemId) { return getOrCreateCart()
            .flatMap(cart -> {
                Long cid = cart.getId();
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


    public Mono<CartPageData> buildCartPageData() {
        return getOrCreateCart()
                .flatMap(cart ->
                        paymentClient.getBalance()
                                .map(balance -> {
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
                );
    }


    public record CartPageData(
            List<Item> items,
            BigDecimal total,
            boolean empty,
            BigDecimal balance,
            boolean disableBuy
    ) {}
}
