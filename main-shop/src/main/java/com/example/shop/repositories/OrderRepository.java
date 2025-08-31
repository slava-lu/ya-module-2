package com.example.shop.repositories;

import com.example.shop.models.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findByUserId(Long userId);
}
