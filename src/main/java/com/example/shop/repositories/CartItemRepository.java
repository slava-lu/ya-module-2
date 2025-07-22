package com.example.shop.repositories;

import com.example.shop.models.CartItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface CartItemRepository  extends R2dbcRepository<CartItem, Long> {

}