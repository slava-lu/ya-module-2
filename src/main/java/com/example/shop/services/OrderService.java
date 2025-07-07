package com.example.shop.services;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Order;
import com.example.shop.models.OrderItem;
import com.example.shop.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final CartService cartService;

    @Transactional
    public Order buyCart() {
        Cart cart = cartService.getOrCreateCart();

        Order order = new Order();
        for (CartItem ci : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setItem(ci.getItem());
            oi.setCount(ci.getCount());
            oi.setOrder(order);
            order.getItems().add(oi);
        }
        Order saved = orderRepo.save(order);

        cart.getItems().clear();

        return saved;
    }

    public List<Order> findAll() {
        return orderRepo.findAll();
    }

    public Order getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }
}