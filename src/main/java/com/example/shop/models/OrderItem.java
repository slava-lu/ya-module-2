package com.example.shop.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {

    @Id
    private Long id;

    /** FK columns persisted in the DB */
    private Long itemId;
    private Long orderId;

    private int count;

    /** Loaded manually via ItemRepository.findById(itemId) */
    @Transient
    private Item item;

    /** Loaded manually via OrderRepository or service logic */
    @Transient
    private Order order;

    // Convenience getters for Thymeleaf
    public Long getId()          { return item.getId();          }
    public String getTitle()     { return item.getTitle();       }
    public String getDescription(){ return item.getDescription(); }
    public String getImgPath()   { return item.getImgPath();     }
    public BigDecimal getPrice() { return item.getPrice();       }
}
