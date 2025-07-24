package com.example.shop.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cart {

    @Id
    private Long id;

    @Transient
    private List<CartItem> items = new ArrayList<>();

    public BigDecimal getTotal() {
        return items.stream()
                .map(ci -> ci.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(ci.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
