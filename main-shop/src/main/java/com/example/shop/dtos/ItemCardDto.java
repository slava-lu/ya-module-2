package com.example.shop.dtos;

public record ItemCardDto(
        Long id, String imgPath, String title, String description, java.math.BigDecimal price
) implements java.io.Serializable { private static final long serialVersionUID = 1L; }