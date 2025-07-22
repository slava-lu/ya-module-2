package com.example.shop.controllers;

import com.example.shop.models.Item;
import com.example.shop.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Mono<Rendering> showCart() {
        return cartService.getOrCreateCart()
                .map(cart -> {
                    List<Item> items = cart.getItems().stream()
                            .map(ci -> {
                                Item item = ci.getItem();
                                item.setCount(ci.getCount());  // count is @Transient on Item
                                return item;
                            })
                            .collect(Collectors.toList());

                    return Rendering.view("cart")
                            .modelAttribute("items", items)
                            .modelAttribute("total", cart.getTotal())
                            .modelAttribute("empty", cart.isEmpty())
                            .build();
                });
    }

    @PostMapping("/{id}")
    public Mono<String> updateCart(
            @PathVariable Long id,
            @RequestParam String action
    ) {
        Mono<Void> op = switch (action) {
            case "plus"   -> cartService.add(id).then();
            case "minus"  -> cartService.remove(id).then();
            case "delete" -> cartService.delete(id).then();
            default       -> Mono.empty();
        };

        return op.thenReturn("redirect:/cart/items");
    }
}
