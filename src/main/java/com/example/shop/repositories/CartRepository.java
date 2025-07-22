package com.example.shop.repositories;

import com.example.shop.models.Cart;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface CartRepository extends R2dbcRepository<Cart, Long> {
}