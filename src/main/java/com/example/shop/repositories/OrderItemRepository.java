package com.example.shop.repositories;

import com.example.shop.models.OrderItem;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {
}