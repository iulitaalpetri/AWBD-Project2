package com.awbd.bookstore.models;

import jakarta.persistence.*;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private int rating;

    // ÎNLOCUIT: User user cu Long userId
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    public Review() {
    }

    // Constructor modificat să primească userId
    public Review(String content, Long userId, Book book) {
        this.content = content;
        this.userId = userId;
        this.book = book;
    }

    // Constructor cu rating
    public Review(String content, int rating, Long userId, Book book) {
        this.content = content;
        this.rating = rating;
        this.userId = userId;
        this.book = book;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    // MODIFICAT: getters/setters pentru userId
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }
}