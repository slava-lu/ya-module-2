package com.example.shop.services;

import com.example.shop.dtos.ItemCardDto;
import com.example.shop.dtos.ItemListDto;
import com.example.shop.dtos.SimplePage;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final CacheManager cacheManager;

    public Mono<Page<Item>> getItems(String search,
                                     ItemSort sort,
                                     int pageNumber,
                                     int pageSize) {

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, resolveSort(sort));

        if (search == null || search.isBlank()) {
            Mono<Long> total = itemRepository.count();
            Mono<List<Item>> list = itemRepository.findAll(pageable).collectList();

            return Mono.zip(list, total)
                    .map(tuple -> new PageImpl<>(
                            tuple.getT1(),
                            pageable,
                            tuple.getT2()
                    ));
        } else {
            Mono<Long> total = itemRepository
                    .countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search);

            Mono<List<Item>> list = itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable)
                    .collectList();

            return Mono.zip(list, total)
                    .map(tuple -> new PageImpl<>(
                            tuple.getT1(),
                            pageable,
                            tuple.getT2()
                    ));
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

    public SimplePage<ItemListDto> getItemsPageSync(String search, ItemSort sort, int pageNumber, int pageSize) {
        String normalizedSearch = (search == null ? "" : search.trim().toLowerCase());
        String key = normalizedSearch + "|" + sort.name();

        Cache cache = Objects.requireNonNull(cacheManager.getCache("itemListPages"));
        CachedItems cached = cache.get(key, CachedItems.class);
        if (cached == null) {
            cached = new CachedItems();
        }

        if (cached.total < 0) {
            cached.total = normalizedSearch.isEmpty()
                    ? itemRepository.count().block()
                    : itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    normalizedSearch, normalizedSearch
            ).block();
        }

        long startIndex = (long) (pageNumber - 1) * pageSize;
        long needUpTo = Math.min(startIndex + pageSize, cached.total);

        while (cached.items.size() < needUpTo) {
            int offset = cached.items.size();
            int limit = (int) (needUpTo - offset);

            Pageable pageable = PageRequest.of(
                    limit == 0 ? 0 : offset / limit,
                    Math.max(limit, 1),
                    resolveSort(sort)
            );

            List<Item> fetched = normalizedSearch.isEmpty()
                    ? itemRepository.findAll(pageable).collectList().block()
                    : itemRepository
                    .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                            normalizedSearch, normalizedSearch, pageable
                    )
                    .collectList().block();

            if (fetched == null || fetched.isEmpty()) break;

            for (Item it : fetched) {
                cached.items.add(new ItemListDto(
                        it.getId(),
                        it.getTitle(),
                        it.getDescription(),
                        it.getPrice(),
                        it.getImgPath()
                ));
                if (cached.items.size() >= cached.total) break;
                if (cached.items.size() >= needUpTo) break;
            }
        }

        cache.put(key, cached);

        int from = (int) startIndex;
        int to = (int) Math.min(startIndex + pageSize, cached.items.size());
        List<ItemListDto> slice = from < cached.items.size() ? cached.items.subList(from, to) : List.of();

        return new SimplePage<>(new ArrayList<>(slice), pageNumber, pageSize, cached.total);
    }

    private Sort resolveSort(ItemSort sort) {
        return switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            case NO -> Sort.unsorted();
        };
    }

    private static final class CachedItems implements Serializable {
        private static final long serialVersionUID = 1L; // Recommended to add
        List<ItemListDto> items = new ArrayList<>();
        long total = -1;
    }
}