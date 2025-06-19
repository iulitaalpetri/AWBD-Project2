package com.awbd.bookstore.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue
    private long id;
    private String name;
    private String description;

    @OneToMany(mappedBy = "category")
    private List<Book> books;

    @ManyToMany(mappedBy = "categories")
    private List<Sale> sales = new ArrayList<>();

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }
}