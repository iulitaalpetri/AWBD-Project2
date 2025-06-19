package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Wishlist w JOIN w.books b WHERE w.user.id = :userId AND b.id = :bookId")
    boolean existsBookInWishlist(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("SELECT COUNT(b) FROM Wishlist w JOIN w.books b WHERE w.id = :wishlistId")
    long countBooks(@Param("wishlistId") Long wishlistId);

    @Query("SELECT book FROM Wishlist w JOIN w.books book WHERE w.id = :wishlistId")
    List<Book> findBooksByWishlistId(@Param("wishlistId") Long wishlistId);

}