package com.example.shop.controllers;

import com.example.shop.models.Cart;
import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

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
    public Mono<Rendering> showItems(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") ItemSort sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber
    ) {
        return itemService.getItems(search, sort, pageNumber, pageSize)
                .zipWith(cartService.getOrCreateCart())
                .map(tuple -> {
                    var page = tuple.getT1();
                    var cart = tuple.getT2();

                    Map<Long,Integer> counts = cart.getItems().stream()
                            .collect(Collectors.toMap(ci -> ci.getItem().getId(), CartItem::getCount));
                    page.getContent().forEach(item ->
                            item.setCount(counts.getOrDefault(item.getId(), 0))
                    );

                    List<List<Item>> rows = new ArrayList<>();
                    var row = new ArrayList<Item>();
                    for (var it : page.getContent()) {
                        row.add(it);
                        if (row.size() == 3) {
                            rows.add(row);
                            row = new ArrayList<>();
                        }
                    }
                    if (!row.isEmpty()) rows.add(row);

                    return Rendering.view("main")
                            .modelAttribute("items", rows)
                            .modelAttribute("search", search)
                            .modelAttribute("sort", sort)
                            .modelAttribute("paging", new Object() {
                                public int pageNumber() { return pageNumber; }
                                public int pageSize()   { return pageSize; }
                                public boolean hasPrevious() { return page.hasPrevious(); }
                                public boolean hasNext()     { return page.hasNext(); }
                            })
                            .build();
                });
    }

    @PostMapping("/main/items/{id}")
    public Mono<String> updateCount(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") ItemSort sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber
    ) {
        Mono<Void> op = "plus".equals(action)
                ? cartService.add(id).then()
                : cartService.remove(id).then();

        return op.thenReturn(
                String.format("redirect:/main/items?search=%s&sort=%s&pageSize=%d&pageNumber=%d",
                        search, sort, pageSize, pageNumber)
        );
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> showItem(
            @PathVariable Long id
    ) {
        return itemService.getById(id)
                .zipWith(cartService.getOrCreateCart())
                .map(tuple -> {
                    var item = tuple.getT1();
                    var cart = tuple.getT2();

                    int cnt = cart.getItems().stream()
                            .filter(ci -> ci.getItem().getId().equals(id))
                            .findFirst()
                            .map(CartItem::getCount)
                            .orElse(0);
                    item.setCount(cnt);

                    return Rendering.view("item")
                            .modelAttribute("item", item)
                            .build();
                });
    }

    @PostMapping("/items/{id}")
    public Mono<String> updateItemCount(
            @PathVariable Long id,
            @RequestParam String action
    ) {
        Mono<Void> op = "plus".equals(action)
                ? cartService.add(id).then()
                : cartService.remove(id).then();

        return op.thenReturn("redirect:/items/" + id);
    }
}
