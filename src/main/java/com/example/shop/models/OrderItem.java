package com.example.shop.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Item item;

    private int count;

    @ManyToOne
    @JoinColumn(name="order_id")
    private Order order;

    // Expose Item fields directly so template can do item.getTitle(), etc.
    public Long getId()         { return item.getId();        }
    public String getTitle()    { return item.getTitle();     }
    public String getDescription(){ return item.getDescription();}
    public String getImgPath()  { return item.getImgPath();   }
    public BigDecimal getPrice(){ return item.getPrice();       }

}
