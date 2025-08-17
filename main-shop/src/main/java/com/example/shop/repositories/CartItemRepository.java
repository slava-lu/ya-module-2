package com.example.shop.repositories;

import com.example.shop.models.CartItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface CartItemRepository  extends R2dbcRepository<CartItem, Long> {
    Flux<CartItem> findByCartId(Long cartId);
}