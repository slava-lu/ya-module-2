package com.example.shop.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CartItem {

    @Id
    private Long id;

    /**
     * Foreign key columns that actually get stored
     */
    private Long itemId;
    private Long cartId;

    private int count;

    @Transient
    private Item item;

    @Transient
    private Cart cart;
}