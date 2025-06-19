package com.awbd.bookstore.DTOs;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private String content;
    private int rating;

    private Long userId;
    private Long bookId;
    private String username;

    public ReviewDTO(String content, int rating, Long userId, Long bookId) {
        this.content = content;
        this.rating = rating;
        this.username = username;
        this.userId = userId;
        this.bookId = bookId;
    }
}