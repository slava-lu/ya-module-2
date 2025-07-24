package com.example.shop.services;

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
import org.springframework.data.domain.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private List<Item> content;
    private Page<Item> samplePage;

    @BeforeEach
    void setup() {
        Item i1 = new Item(1L, "Title1", "Desc1", "/img1", BigDecimal.ONE, 0);
        Item i2 = new Item(2L, "Title2", "Desc2", "/img2", BigDecimal.TEN, 0);
        content = List.of(i1, i2);
        samplePage = new PageImpl<>(content, PageRequest.of(0, 10), content.size());
    }


    @Test
    void getItems_withBlankSearch_usesFindAll() {
        Pageable expected = PageRequest.of(0, 3, Sort.unsorted());
        when(itemRepository.count()).thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findAll(expected))
                .thenReturn(Flux.fromIterable(content));

        Page<Item> result = itemService
                .getItems("   ", ItemSort.NO, 1, 3)
                .block();

        assertThat(result).isEqualTo(new PageImpl<>(content, expected, content.size()));
        verify(itemRepository).findAll(any(Pageable.class));
    }

    @Test
    void getItems_withSearch_usesFindByTitleOrDescription() {
        Pageable expected = PageRequest.of(0, 10, Sort.unsorted());
        when(itemRepository.countByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo"))
                .thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo", expected))
                .thenReturn(Flux.fromIterable(content));

        Page<Item> result = itemService
                .getItems("foo", ItemSort.NO, 1, 10)
                .block();

        assertThat(result).isEqualTo(samplePage);
        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo", expected);
    }

    @Test
    void getItems_sortAlpha_sortsByTitle() {
        Pageable expected = PageRequest.of(2, 7, Sort.by("title").ascending());
        when(itemRepository.count()).thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findAll(expected))
                .thenReturn(Flux.fromIterable(content));

        itemService.getItems("", ItemSort.ALPHA, 3, 7).block();

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(cap.capture());
        assertThat(cap.getValue().getSort()).isEqualTo(Sort.by("title").ascending());
    }

    @Test
    void getItems_sortPrice_sortsByPrice() {
        Pageable expected = PageRequest.of(3, 2, Sort.by("price").ascending());
        when(itemRepository.count()).thenReturn(Mono.just((long) content.size()));
        when(itemRepository.findAll(expected))
                .thenReturn(Flux.fromIterable(content));

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
}
