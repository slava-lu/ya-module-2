package com.example.shop.services;

import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Mono<Page<Item>> getItems(String search,
                                     ItemSort sort,
                                     int pageNumber,
                                     int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, resolveSort(sort));

        if (search == null || search.isBlank()) {
            Mono<Long> total = itemRepository.count();
            Mono<List<Item>> list = itemRepository
                    .findAll(pageable)
                    .collectList();

            return Mono.zip(list, total)
                    .map(tuple ->
                            new PageImpl<>(
                                    tuple.getT1(),
                                    pageable,
                                    tuple.getT2()
                            )
                    );
        } else {
            Mono<Long> total = itemRepository
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);

            Mono<List<Item>> list = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable)
                    .collectList();

            return Mono.zip(list, total)
                    .map(tuple ->
                            new PageImpl<>(
                                    tuple.getT1(),
                                    pageable,
                                    tuple.getT2()
                            )
                    );
        }
    }

    public Mono<Item> getById(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Item not found: " + id)));
    }

    private Sort resolveSort(ItemSort sort) {
        return switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO    -> Sort.unsorted();
        };
    }
}
