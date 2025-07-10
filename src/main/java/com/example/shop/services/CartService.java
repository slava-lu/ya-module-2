package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.CartRepository;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ItemRepository itemRepo;

    private Long demoCartId = null;

    @Transactional
    public Cart getOrCreateCart() {
        if (demoCartId != null) {
            return cartRepo.findById(demoCartId)
                    .orElseThrow(() -> new IllegalStateException("Demo cart was deleted"));
        }
        Cart newCart = new Cart();
        Cart saved = cartRepo.save(newCart);
        demoCartId = saved.getId();
        return saved;
    }

    @Transactional
    public void add(Long itemId) {
        Cart cart = getOrCreateCart();                           // managed
        Item item = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("No item " + itemId));
        CartItem ci = cart.getItems().stream()
                .filter(x -> x.getItem().getId().equals(itemId))
                .findFirst()
                .orElseGet(() -> {
                    CartItem newCi = new CartItem();
                    newCi.setItem(item);
                    newCi.setCount(0);
                    newCi.setCart(cart);
                    cart.getItems().add(newCi);
                    return newCi;
                });
        ci.setCount(ci.getCount() + 1);
    }

    @Transactional
    public void remove(Long itemId) {
        Cart cart = getOrCreateCart();
        cart.getItems().stream()
                .filter(x -> x.getItem().getId().equals(itemId))
                .findFirst()
                .ifPresent(ci -> {
                    if (ci.getCount() > 1) {
                        ci.setCount(ci.getCount() - 1);
                    } else {
                        cart.getItems().remove(ci);
                        cartItemRepo.delete(ci);
                    }
                });
    }

    @Transactional
    public void delete(Long itemId) {
        Cart cart = getOrCreateCart();
        cart.getItems().stream()
                .filter(x -> x.getItem().getId().equals(itemId))
                .findFirst()
                .ifPresent(ci -> {
                    cart.getItems().remove(ci);
                    cartItemRepo.delete(ci);
                });
    }
}