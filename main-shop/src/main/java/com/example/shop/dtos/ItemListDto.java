package com.example.shop.dtos;

public record ItemListDto(
        Long id,
        String title,
        String description,
        java.math.BigDecimal price,
        String imgPath
) implements java.io.Serializable { private static final long serialVersionUID = 1L; }
