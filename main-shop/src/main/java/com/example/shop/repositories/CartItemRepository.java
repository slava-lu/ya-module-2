package com.example.shop.repositories;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CartItemRepository  extends R2dbcRepository<CartItem, Long> {
    Flux<CartItem> findByCartId(Long cartId);
}