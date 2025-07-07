package com.example.shop.controllers;

import com.example.shop.models.Cart;
import com.example.shop.models.Item;
import com.example.shop.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public String showCart(Model model) {
        Cart cart = cartService.getOrCreateCart();


        List<Item> items = cart.getItems().stream()
                .map(ci -> {
                    Item item = ci.getItem();
                    item.setCount(ci.getCount());  // requires @Transient count on Item
                    return item;
                })
                .collect(Collectors.toList());

        model.addAttribute("items", items);
        model.addAttribute("total", cart.getTotal());
        model.addAttribute("empty", cart.isEmpty());
        return "cart";
    }

    @PostMapping("/{id}")
    public String updateCart(
            @PathVariable Long id,
            @RequestParam String action
    ) {
        switch (action) {
            case "plus"   -> cartService.add(id);
            case "minus"  -> cartService.remove(id);
            case "delete" -> cartService.delete(id);
        }
        return "redirect:/cart/items";
    }

}
