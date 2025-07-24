package com.example.shop.services;

import com.example.shop.models.Order;
import com.example.shop.repositories.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepo;

    @InjectMocks
    private OrderService orderService;

    @Test
    void findAll_returnsAllOrders() {
        Order o1 = new Order(); o1.setId(1L);
        Order o2 = new Order(); o2.setId(2L);

        when(orderRepo.findAll()).thenReturn(Flux.just(o1, o2));

        List<Order> result = orderService.findAll()
                .collectList()
                .block();

        assertThat(result).containsExactly(o1, o2);
        verify(orderRepo).findAll();
    }

    @Test
    void getById_missing_throwsException() {
        when(orderRepo.findById(99L)).thenReturn(Mono.empty());

        assertThatThrownBy(() -> orderService.getById(99L).block())
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found: 99");
    }

}
