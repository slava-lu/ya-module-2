package com.example.shop.repositories;

import com.example.shop.models.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface ItemRepository extends R2dbcRepository<Item, Long> {
    Flux<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );
}