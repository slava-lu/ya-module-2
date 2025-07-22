package com.example.shop.repositories;

import com.example.shop.models.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
}
