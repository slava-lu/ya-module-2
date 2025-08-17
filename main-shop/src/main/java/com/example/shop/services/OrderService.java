package com.example.shop.services;

import com.example.shop.models.CartItem;
import com.example.shop.models.Order;
import com.example.shop.models.OrderItem;
import com.example.shop.repositories.CartItemRepository;
import com.example.shop.repositories.OrderItemRepository;
import com.example.shop.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository       orderRepo;
    private final OrderItemRepository   orderItemRepo;
    private final CartService           cartService;
    private final CartItemRepository    cartItemRepo;
    private final PaymentServiceClient paymentServiceClient;

    public Mono<Order> buyCart() {
        return cartService.getOrCreateCart()
                .flatMap(cart -> {
                    Order order = new Order();
                    for (CartItem ci : cart.getItems()) {
                        OrderItem oi = new OrderItem();
                        oi.setItemId(ci.getItemId());
                        oi.setItem(ci.getItem());
                        oi.setCount(ci.getCount());
                        order.getItems().add(oi);
                    }
                    order.computeTotal();

                    return paymentServiceClient.pay(order.getTotal())
                            .flatMap(paymentResponse -> {
                                return orderRepo.save(order)
                                        .flatMap(savedOrder ->
                                                Flux.fromIterable(order.getItems())
                                                        .doOnNext(oi -> oi.setOrderId(savedOrder.getId()))
                                                        .flatMap(orderItemRepo::save)
                                                        .then()
                                                        .thenReturn(savedOrder)
                                        )
                                        .flatMap(savedOrder ->
                                                cartItemRepo.findByCartId(cart.getId())
                                                        .flatMap(cartItemRepo::delete)
                                                        .then()
                                                        .thenReturn(savedOrder)
                                        );
                            })
                            .onErrorResume(e -> Mono.error(new IllegalStateException(
                                    "Payment declined. Reason: " + e.getMessage())));
                });
    }

    public Flux<Order> findAll() {
        return orderRepo.findAll();
    }

    public Mono<Order> getById(Long id) {
        return orderRepo.findById(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found: " + id)));
    }
}
