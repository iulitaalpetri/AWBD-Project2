package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByTitleContaining(String title);
    List<Book> findByCategoryId(Long categoryId);
    List<Book> findByAuthorId(Long authorId);

    // actualizare stoc dupa comanda
    @Modifying
    @Transactional
    @Query("UPDATE Book b SET b.stock = b.stock - :quantity WHERE b.id = :bookId AND b.stock >= :quantity")
    int updateStock(@Param("bookId") Long bookId, @Param("quantity") int quantity);

    // update pentru admin
    @Modifying
    @Transactional
    @Query("UPDATE Book b SET b.title = :title, b.price = :price, b.stock = :stock, b.category.id = :categoryId, b.author.id = :authorId WHERE b.id = :id")
    void updateBookDetails(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("price") double price,
            @Param("stock") int stock,
            @Param("categoryId") Long categoryId,
            @Param("authorId") Long authorId
    );


    // filtru dupa pret
    List<Book> findByPriceLessThan(double maxPrice);
    List<Book> findByPriceBetween(double minPrice, double maxPrice);

    List<Book> findByStockGreaterThan(int stock);

    // nr review
    @Query("SELECT COUNT(r) FROM Book b JOIN b.reviews r WHERE b.id = :bookId")
    long countReviews(@Param("bookId") Long bookId);

    // rating mediu
    @Query("SELECT AVG(r.rating) FROM Book b JOIN b.reviews r WHERE b.id = :bookId")
    Double getAverageRating(@Param("bookId") Long bookId);
}