package com.example.shop.services;

import com.example.shop.models.*;
import com.example.shop.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartService cartService;
    private final CartItemRepository cartItemRepo;
    private final UserRepository userRepo; // Add UserRepository
    private final PaymentServiceClient paymentServiceClient;

    // Helper method to get the current application user
    private Mono<User> getCurrentUser(UserDetails userDetails) {
        return userRepo.findByEmail(userDetails.getUsername());
    }

    @Transactional
    public Mono<Order> buyCart(UserDetails userDetails) {
        Mono<User> userMono = getCurrentUser(userDetails);
        Mono<Cart> cartMono = cartService.getOrCreateCart(userDetails);

        return Mono.zip(userMono, cartMono)
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Cart cart = tuple.getT2();

                    Order order = new Order();
                    order.setUserId(user.getId()); // Set the user ID on the order
                    for (CartItem ci : cart.getItems()) {
                        OrderItem oi = new OrderItem();
                        oi.setItemId(ci.getItemId());
                        oi.setItem(ci.getItem());
                        oi.setCount(ci.getCount());
                        order.getItems().add(oi);
                    }
                    order.computeTotal();

                    return paymentServiceClient.pay(order.getTotal())
                            .flatMap(paymentResponse -> orderRepo.save(order)
                                    .flatMap(savedOrder ->
                                            Flux.fromIterable(order.getItems())
                                                    .doOnNext(oi -> oi.setOrderId(savedOrder.getId()))
                                                    .flatMap(orderItemRepo::save)
                                                    .then(cartItemRepo.deleteAll(cart.getItems())) // Clear cart items
                                                    .thenReturn(savedOrder)
                                    ))
                            .onErrorResume(e -> Mono.error(new IllegalStateException(
                                    "Payment declined. Reason: " + e.getMessage())));
                });
    }

    public Flux<Order> findAllForUser(UserDetails userDetails) {
        return getCurrentUser(userDetails)
                .flatMapMany(user -> orderRepo.findByUserId(user.getId()));
    }

    public Mono<Order> findByIdForUser(Long id, UserDetails userDetails) {
        return getCurrentUser(userDetails)
                .flatMap(user -> orderRepo.findById(id)
                        .filter(order -> order.getUserId().equals(user.getId())) // Ensure order belongs to user
                )
                .switchIfEmpty(Mono.error(new NoSuchElementException("Order not found or access denied: " + id)));
    }
}