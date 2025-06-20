package com.awbd.bookstore.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orders")  // order este cuv rezervat in SQL
@Getter
@Setter
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // ÎNLOCUIT: User user cu Long userId
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToMany
    @JoinTable(
            name = "order_book",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books = new HashSet<>();

    private LocalDateTime orderDate;

    private Double totalPrice;

    // only one sale per order
    @OneToOne
    @JoinColumn(name = "sale_id", unique = true)
    private Sale sale;

    // Bloc de inițializare
    {
        this.orderDate = LocalDateTime.now();
    }

    // Constructor modificat să primească userId
    public Order(Long userId) {
        this.userId = userId;
        this.orderDate = LocalDateTime.now();
    }

    public void addBook(Book book) {
        this.books.add(book);
    }

    public void removeBook(Book book) {
        this.books.remove(book);
    }

    // Metodă simplificată
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}