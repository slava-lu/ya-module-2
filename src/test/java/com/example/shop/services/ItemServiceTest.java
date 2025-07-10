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

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Page<Item> samplePage;

    @BeforeEach
    void setup() {
        Item i1 = new Item(1L, "Title1", "Desc1", "/img1", BigDecimal.ONE, 0);
        Item i2 = new Item(2L, "Title2", "Desc2", "/img2", BigDecimal.TEN, 0);
        samplePage = new PageImpl<>(List.of(i1, i2));
    }

    @Test
    void getItems_withNullSearch_usesFindAll() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(samplePage);
        Page<Item> result = itemService.getItems(null, ItemSort.NO, 2, 5);
        assertThat(result).isSameAs(samplePage);
        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(cap.capture());
        Pageable p = cap.getValue();
        assertThat(p.getPageNumber()).isEqualTo(1);
        assertThat(p.getPageSize()).isEqualTo(5);
        assertThat(p.getSort()).isEqualTo(Sort.unsorted());
    }

    @Test
    void getItems_withBlankSearch_usesFindAll() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(samplePage);
        Page<Item> result = itemService.getItems("   ", ItemSort.NO, 1, 3);
        assertThat(result).isSameAs(samplePage);
        verify(itemRepository).findAll(any(Pageable.class));
    }

    @Test
    void getItems_withSearch_usesFindByTitleOrDescription() {
        when(itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(samplePage);
        Page<Item> result = itemService.getItems("foo", ItemSort.NO, 1, 10);
        assertThat(result).isSameAs(samplePage);
        verify(itemRepository).findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase("foo", "foo", PageRequest.of(0, 10, Sort.unsorted()));
    }

    @Test
    void getItems_sortAlpha_sortsByTitle() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(samplePage);
        itemService.getItems("", ItemSort.ALPHA, 3, 7);
        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(cap.capture());
        assertThat(cap.getValue().getSort()).isEqualTo(Sort.by("title").ascending());
    }

    @Test
    void getItems_sortPrice_sortsByPrice() {
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(samplePage);
        itemService.getItems("", ItemSort.PRICE, 4, 2);
        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(itemRepository).findAll(cap.capture());
        assertThat(cap.getValue().getSort()).isEqualTo(Sort.by("price").ascending());
    }

    @Test
    void getById_existing_returnsItem() {
        Item item = new Item(5L, "X", "Y", "/img", BigDecimal.ZERO, 0);
        when(itemRepository.findById(5L)).thenReturn(Optional.of(item));
        Item result = itemService.getById(5L);
        assertThat(result).isSameAs(item);
    }

    @Test
    void getById_missing_throwsException() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> itemService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Item not found: 99");
    }
}
