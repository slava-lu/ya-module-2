package com.example.shop.controllers;

import com.example.shop.dto.ItemViewModel;
import com.example.shop.models.Item;
import com.example.shop.models.ItemSort;
import com.example.shop.services.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/main/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    public String showItems(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "NO") ItemSort sort,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "1") int pageNumber,
            Model model) {

        Page<Item> page = itemService.getItems(search, sort, pageNumber, pageSize);

        List<ItemViewModel> itemVMs = page.getContent().stream()
                .map(item -> new ItemViewModel(item, getItemCountInCart(item.getId())))
                .toList();

        // Group into rows
        int itemsPerRow = 3;
        List<List<ItemViewModel>> groupedItems = new ArrayList<>();
        List<ItemViewModel> row = new ArrayList<>();
        for (ItemViewModel vm : itemVMs) {
            row.add(vm);
            if (row.size() == itemsPerRow) {
                groupedItems.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            groupedItems.add(row);
        }

        model.addAttribute("items", groupedItems);
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
    private int getItemCountInCart(Long itemId) {
        return 0;
    }
}
