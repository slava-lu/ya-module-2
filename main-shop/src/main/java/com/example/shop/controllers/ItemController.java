package com.example.shop.controllers;

import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.services.CartService;
import com.example.shop.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        // Use cached synchronous page but call it off the event loop
        return Mono.fromCallable(() -> itemService.getItemsPageSync(search, sort, pageNumber, pageSize))
                .subscribeOn(Schedulers.boundedElastic())
                .zipWith(cartService.getOrCreateCart())
                .map(tuple -> {
                    var page = tuple.getT1(); // SimplePage<ItemListDto>
                    var cart = tuple.getT2();

                    Map<Long, Integer> counts = cart.getItems().stream()
                            .collect(Collectors.toMap(ci -> ci.getItem().getId(), CartItem::getCount));

                    // convert DTOs to lightweight Items so the existing Thymeleaf "main" template works
                    List<Item> flatItems = page.content().stream().map(dto -> {
                        Item it = new Item();
                        it.setId(dto.id());
                        it.setTitle(dto.title());
                        it.setDescription(dto.description());
                        it.setImgPath(dto.imgPath());
                        it.setPrice(dto.price());
                        it.setCount(counts.getOrDefault(dto.id(), 0));
                        return it;
                    }).toList();

                    // group into rows of 3
                    List<List<Item>> rows = new ArrayList<>();
                    var row = new ArrayList<Item>();
                    for (var it : flatItems) {
                        row.add(it);
                        if (row.size() == 3) {
                            rows.add(row);
                            row = new ArrayList<>();
                        }
                    }
                    if (!row.isEmpty()) rows.add(row);


                    boolean hasPrev = page.pageNumber() > 1;
                    long startIndex = (long) (page.pageNumber() - 1) * page.pageSize();
                    boolean hasNext = startIndex + flatItems.size() < page.totalElements();

                    return Rendering.view("main")
                            .modelAttribute("items", rows)
                            .modelAttribute("search", search)
                            .modelAttribute("sort", sort)
                            .modelAttribute("paging", new Object() {
                                public int pageNumber() { return page.pageNumber(); }
                                public int pageSize() { return page.pageSize(); }
                                public boolean hasPrevious() { return hasPrev; }
                                public boolean hasNext() { return hasNext; }
                            })
                            .build();
                });
    }

    @PostMapping(value = "/main/items/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<String> updateCount(
            @PathVariable("id") Long id,
            @RequestParam("action") String action,
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
    public Mono<Rendering> showItem(@PathVariable Long id) {
        // Use cached synchronous item card but call it off the event loop
        return Mono.fromCallable(() -> itemService.getItemCardSync(id))
                .subscribeOn(Schedulers.boundedElastic())
                .zipWith(cartService.getOrCreateCart())
                .map(tuple -> {
                    var dto  = tuple.getT1(); // ItemCardDto
                    var cart = tuple.getT2();

                    int cnt = cart.getItems().stream()
                            .filter(ci -> ci.getItem().getId().equals(id))
                            .findFirst()
                            .map(CartItem::getCount)
                            .orElse(0);

                    // Build lightweight Item to satisfy your Thymeleaf view (getters used in template)
                    Item vm = new Item();
                    vm.setId(dto.id());
                    vm.setImgPath(dto.imgPath());
                    vm.setTitle(dto.title());
                    vm.setDescription(dto.description());
                    vm.setPrice(dto.price());
                    vm.setCount(cnt);

                    return Rendering.view("item")
                            .modelAttribute("item", vm)
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
