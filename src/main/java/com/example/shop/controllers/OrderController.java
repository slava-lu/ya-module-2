package com.example.shop.controllers;

import com.example.shop.models.Order;
import com.example.shop.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public Mono<String> buy() {
        return orderService.buyCart()
                .map(newOrder ->
                        "redirect:/orders/" + newOrder.getId() + "?newOrder=true"
                );
    }

    @GetMapping("/orders")
    public Mono<Rendering> listOrders() {
        return orderService.findAll()
                .collectList()
                .map(orders ->
                        Rendering.view("orders")
                                .modelAttribute("orders", orders)
                                .build()
                );
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> showOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean newOrder
    ) {
        return orderService.getById(id)
                .map(order ->
                        Rendering.view("order")
                                .modelAttribute("order", order)
                                .modelAttribute("newOrder", newOrder)
                                .build()
                );
    }
}
