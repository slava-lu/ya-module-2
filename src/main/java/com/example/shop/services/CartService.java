package com.example.shop.services;

import com.example.shop.models.CartItem;
import com.example.shop.models.Item;
import com.example.shop.repositories.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CartService {

    private final Map<Long, CartItem> items = new HashMap<>();
    private final ItemRepository itemRepository;

    public int getCount(Long itemId) {
        CartItem ci = items.get(itemId);
        return (ci != null ? ci.getCount() : 0);
    }

    public void add(Long itemId) {
        items.compute(itemId, (id, ci) -> {
            if (ci == null) {
                ci = new CartItem();
                Item item = itemRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
                ci.setItem(item);
                ci.setCount(0);
            }
            ci.setCount(ci.getCount() + 1);
            return ci;
        });
    }

    public void remove(Long itemId) {
        items.computeIfPresent(itemId, (id, ci) -> {
            int c = ci.getCount() - 1;
            return c > 0 ? ci : null;
        });
    }
}