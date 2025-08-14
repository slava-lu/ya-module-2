package com.example.shop.dtos;

public record SimplePage<T>(
        java.util.List<T> content, int pageNumber, int pageSize, long totalElements
) implements java.io.Serializable { private static final long serialVersionUID = 1L; }
