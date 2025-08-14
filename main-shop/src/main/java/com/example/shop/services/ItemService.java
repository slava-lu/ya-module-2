package com.example.shop.services;

import com.example.shop.dtos.ItemCardDto;
import com.example.shop.dtos.ItemListDto;
import com.example.shop.dtos.SimplePage;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "itemCard", key = "#id")
    public ItemCardDto getItemCardSync(Long id) {
        var item = itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Item not found: " + id)))
                .block();
        return new ItemCardDto(item.getId(), item.getImgPath(), item.getTitle(), item.getDescription(), item.getPrice());
    }

    @Cacheable(
            value = "itemListPages",
            key = "T(java.util.Objects).hash((#search==null?'':#search.trim().toLowerCase())) + ',' + #sort.name() + ',' + #pageNumber + ',' + #pageSize"
    )
    public SimplePage<ItemListDto> getItemsPageSync(String search, ItemSort sort, int pageNumber, int pageSize) {
        var pageable = PageRequest.of(pageNumber - 1, pageSize, resolveSort(sort));

        List<Item> items;
        long total;

        if (search == null || search.isBlank()) {
            total = itemRepository.count().block();
            items = itemRepository.findAll(pageable).collectList().block();
        } else {
            total = itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search).block();
            items = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable)
                    .collectList().block();
        }

        var content = items.stream()
                .map(it -> new ItemListDto(
                        it.getId(),
                        it.getTitle(),
                        it.getDescription(),
                        it.getPrice(),
                        it.getImgPath()
                ))
                .toList();

        return new SimplePage<>(content, pageNumber, pageSize, total);
    }

    private Sort resolveSort(ItemSort sort) {
        return switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO    -> Sort.unsorted();
        };
    }
}
