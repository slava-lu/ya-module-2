package com.example.shop.repositories;

import com.example.shop.models.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ItemRepository extends R2dbcRepository<Item, Long> {
    Flux<Item> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );

    Mono<Long> countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description
    );

    default Flux<Item> findAll(Pageable pageable) {
        Sort sort = pageable.getSort();
        return this.findAll(sort)
                .skip(pageable.getOffset())
                .take(pageable.getPageSize());
    }
}