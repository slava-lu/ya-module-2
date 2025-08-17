package com.example.shop.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table("orders")
public class Order {

    @Id
    private Long id;

    @Transient
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal total;

    public void computeTotal() {
        this.total = items.stream()
                .map(oi -> oi.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(oi.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long id() { return id; }
    public List<OrderItem> items() { return items; }
    public BigDecimal totalSum() { return total; }
}
