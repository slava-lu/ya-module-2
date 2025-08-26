package com.example.shop.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table("users")
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
    private String password;
}