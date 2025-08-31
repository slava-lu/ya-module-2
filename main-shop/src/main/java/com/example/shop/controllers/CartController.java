package com.example.shop.controllers;

import com.example.shop.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Mono<Rendering> showCart(@AuthenticationPrincipal UserDetails userDetails) {
        return cartService.buildCartPageData(userDetails)
                .map(vm -> Rendering.view("cart")
                        .modelAttribute("items", vm.items())
                        .modelAttribute("total", vm.total())
                        .modelAttribute("empty", vm.empty())
                        .modelAttribute("balance", vm.balance())
                        .modelAttribute("disableBuy", vm.disableBuy())
                        .build()
                );
    }

    @PostMapping("/{id}")
    public Mono<String> updateCart(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String action
    ) {
        Mono<Void> op = switch (action) {
            case "plus"   -> cartService.add(id, userDetails).then();
            case "minus"  -> cartService.remove(id, userDetails).then();
            case "delete" -> cartService.delete(id, userDetails).then();
            default       -> Mono.empty();
        };

        return op.thenReturn("redirect:/cart/items");
    }
}