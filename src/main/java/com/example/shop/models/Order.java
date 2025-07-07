package com.example.shop.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal total;

    @PrePersist
    public void prePersist() {
        this.total =  items.stream()
                .map(ci -> ci.getItem().getPrice().multiply(BigDecimal.valueOf(ci.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // For Thymeleaf convenience (template calls order.id(), order.items())
    public Long id() { return id; }
    public List<OrderItem> items() { return items; }
    public BigDecimal totalSum() { return total; }

}
