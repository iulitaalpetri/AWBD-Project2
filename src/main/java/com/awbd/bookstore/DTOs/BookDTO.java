package com.awbd.bookstore.DTOs;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookDTO {
    private Long id;
    private String title;
    private double price;
    private int stock;
    private Long categoryId;
    private Long authorId;
    private String author;

    public BookDTO(String title, double price, int stock, Long categoryId, Long authorId) {
        this.title = title;
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
        this.authorId = authorId;
    }

//    public void setAuthor(String author) {
//        this.authorId = Long.parseLong(author);
//    }
}