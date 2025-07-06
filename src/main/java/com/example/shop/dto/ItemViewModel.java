package com.example.shop.dto;

import com.example.shop.models.Item;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class ItemViewModel {

    private final Item item;
    private final int count;

    public Long getId() {
        return item.getId();
    }

    public String getTitle() {
        return item.getTitle();
    }

    public String getDescription() {
        return item.getDescription();
    }

    public String getImgPath() {
        return item.getImgPath();
    }

    public BigDecimal getPrice() {
        return item.getPrice();
    }
}