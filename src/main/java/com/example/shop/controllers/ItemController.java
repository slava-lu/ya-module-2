package com.example.shop.controllers;
import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping("/")
    public String redirectToMainItems() {
        return "redirect:/main/items";
    }

    @GetMapping("/main/items")
    public String showItems(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") ItemSort sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber,
            Model model
    ) {
        Page<Item> page = itemService.getItems(search, sort, pageNumber, pageSize);
        Cart cart = cartService.getOrCreateCart();
        Map<Long,Integer> counts = cart.getItems().stream()
                .collect(Collectors.toMap(ci -> ci.getItem().getId(), CartItem::getCount));
        page.getContent().forEach(item ->
                item.setCount(counts.getOrDefault(item.getId(), 0))
        );

        // group into rows of 3
        List<List<Item>> rows = new ArrayList<>();
        List<Item> row = new ArrayList<>();
        for (Item it : page.getContent()) {
            row.add(it);
            if (row.size() == 3) {
                rows.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) rows.add(row);

        model.addAttribute("items", rows);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", new Object() {
            public int pageNumber() { return pageNumber; }
            public int pageSize() { return pageSize; }
            public boolean hasPrevious() { return page.hasPrevious(); }
            public boolean hasNext() { return page.hasNext(); }
        });

        return "main";
    }

    @PostMapping("/main/items/{id}")
    public String updateCount(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") ItemSort sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber
    ) {
        if ("plus".equals(action)) {
            cartService.add(id);
        } else if ("minus".equals(action)) {
            cartService.remove(id);
        }
        return String.format("redirect:/main/items?search=%s&sort=%s&pageSize=%d&pageNumber=%d",
                search, sort, pageSize, pageNumber);
    }
}
