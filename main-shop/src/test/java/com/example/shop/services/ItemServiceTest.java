package com.example.shop.services;

import com.example.shop.dtos.ItemCardDto;
import com.example.shop.dtos.ItemListDto;
import com.example.shop.dtos.SimplePage;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.repositories.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private ItemService itemService;

    private List<Item> content;

    @BeforeEach
    void setup() {
        Item i1 = new Item(1L, "Title1", "Desc1", "/img1", BigDecimal.ONE, 0);
        Item i2 = new Item(2L, "Title2", "Desc2", "/img2", BigDecimal.TEN, 0);
        content = List.of(i1, i2);
    }

    @Test
    void getItems_withBlankSearch_usesFindAll() {
        Pageable expectedPageable = PageRequest.of(0, 3, Sort.unsorted());
        when(itemRepository.count()).thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findAll(expectedPageable)).thenReturn(Flux.fromIterable(content));

        Page<Item> result = itemService.getItems("   ", ItemSort.NO, 1, 3).block();

        assertThat(result.getContent()).isEqualTo(content);
        verify(itemRepository).findAll(any(Pageable.class));
    }

    @Test
    void getItems_withSearch_usesFindByTitleOrDescription() {
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.unsorted());
        when(itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo"))
                .thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo", expectedPageable))
                .thenReturn(Flux.fromIterable(content));

        Page<Item> result = itemService.getItems("foo", ItemSort.NO, 1, 10).block();

        assertThat(result.getContent()).isEqualTo(content);
        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo", expectedPageable);
    }

    @Test
    void getItems_sortAlpha_sortsByTitle() {
        when(itemRepository.count()).thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(Flux.fromIterable(content));

        itemService.getItems("", ItemSort.ALPHA, 3, 7).block();

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(cap.capture());
        assertThat(cap.getValue().getSort()).isEqualTo(Sort.by("title").ascending());
    }

    @Test
    void getItems_sortPrice_sortsByPrice() {
        when(itemRepository.count()).thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(Flux.fromIterable(content));

        itemService.getItems("", ItemSort.PRICE, 4, 2).block();

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(cap.capture());
        assertThat(cap.getValue().getSort()).isEqualTo(Sort.by("price").ascending());
    }

    @Test
    void getById_existing_returnsItem() {
        Item item = new Item(5L, "X", "Y", "/img", BigDecimal.ZERO, 0);
        when(itemRepository.findById(5L)).thenReturn(Mono.just(item));

        Item result = itemService.getById(5L).block();

        assertThat(result).isSameAs(item);
    }

    @Test
    void getById_nonExisting_throws() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> itemService.getById(99L).block())
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getItemCardSync_existing_returnsDto() {
        Item item = content.get(0);
        when(itemRepository.findById(1L)).thenReturn(Mono.just(item));

        ItemCardDto result = itemService.getItemCardSync(1L);

        assertThat(result.id()).isEqualTo(item.getId());
        assertThat(result.title()).isEqualTo(item.getTitle());
    }

    @Test
    void getItemCardSync_nonExisting_throws() {
        when(itemRepository.findById(99L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> itemService.getItemCardSync(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Item not found: 99");
    }

    @Test
    void getItemsPageSync_emptyCache_fetchesFromRepoAndCaches() {
        when(cacheManager.getCache("itemListPages")).thenReturn(cache);
        when(cache.get(anyString(), any(Class.class))).thenReturn(null);
        when(itemRepository.count()).thenReturn(Mono.just(2L));
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(Flux.fromIterable(content));

        SimplePage<ItemListDto> result = itemService.getItemsPageSync("", ItemSort.NO, 1, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content().get(0).title()).isEqualTo("Title1");

        verify(itemRepository).count();
        verify(itemRepository).findAll(any(Pageable.class));
        verify(cache).put(eq("|NO"), any());
    }

    @Test
    void getItemsPageSync_fullCacheHit_returnsFromCacheWithoutRepoInteraction() {
        when(cacheManager.getCache("itemListPages")).thenReturn(cache);
        var cachedItems = new ItemService.CachedItems();
        cachedItems.total = 2;
        cachedItems.items.add(new ItemListDto(1L, "Title1", "Desc1", BigDecimal.ONE, "/img1"));
        cachedItems.items.add(new ItemListDto(2L, "Title2", "Desc2", BigDecimal.TEN, "/img2"));
        when(cache.get(eq("|NO"), any(Class.class))).thenReturn(cachedItems);

        SimplePage<ItemListDto> result = itemService.getItemsPageSync("", ItemSort.NO, 1, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);

        verifyNoInteractions(itemRepository);
    }
}

