package com.example.shop.controllers;

import com.example.shop.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public Mono<String> buy(@AuthenticationPrincipal UserDetails userDetails) {
        return orderService.buyCart(userDetails)
                .map(newOrder ->
                        "redirect:/orders/" + newOrder.getId() + "?newOrder=true"
                );
    }

    @GetMapping("/orders")
    public Mono<Rendering> listOrders(@AuthenticationPrincipal UserDetails userDetails) {
        return orderService.findAllForUser(userDetails)
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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "false") boolean newOrder
    ) {
        return orderService.findByIdForUser(id, userDetails)
                .map(order ->
                        Rendering.view("order")
                                .modelAttribute("order", order)
                                .modelAttribute("newOrder", newOrder)
                                .build()
                );
    }
}