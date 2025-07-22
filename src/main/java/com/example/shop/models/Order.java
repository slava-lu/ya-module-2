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
public class Order {

    @Id
    private Long id;

    /**
     * Load/save these via OrderItemRepository.findByOrderId(...)
     */
    @Transient
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Persisted column if you want to store the total.
     * Otherwise you can mark this @Transient and always compute on the fly.
     */
    private BigDecimal total;

    /**
     * Call this before saving (e.g. in your service) to update the stored total.
     */
    public void computeTotal() {
        this.total = items.stream()
                .map(oi -> oi.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(oi.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Thymeleaf convenience
    public Long id() { return id; }
    public List<OrderItem> items() { return items; }
    public BigDecimal totalSum() { return total; }
}
