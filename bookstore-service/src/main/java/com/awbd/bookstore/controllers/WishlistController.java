package com.awbd.bookstore.controllers;

import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.WishlistMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.services.WishlistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.awbd.bookstore.DTOs.BookDTO;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {
    private WishlistService wishlistService;
    private WishlistMapper wishlistMapper;
    private BookMapper bookMapper;
    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);

    // CONSTRUCTOR MODIFICAT - eliminat UserService și JwtUtil
    public WishlistController(WishlistService wishlistService,
                              WishlistMapper wishlistMapper,
                              BookMapper bookMapper) {
        this.wishlistService = wishlistService;
        this.wishlistMapper = wishlistMapper;
        this.bookMapper = bookMapper;
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<BookDTO> addBookToWishlist(
            @RequestHeader("User-Id") Long userId,
            @PathVariable Long bookId) {

        logger.info("Adding book {} to wishlist for user {}", bookId, userId);

        // Folosim metoda createWishlist care verifică automat dacă există
        Wishlist wishlist = wishlistService.createWishlist(userId);

        Book book = wishlistService.addBookToWishlist(wishlist.getId(), bookId);
        logger.info("Book with id: {} added to wishlist with id: {}", bookId, wishlist.getId());

        return ResponseEntity.created(URI.create("/api/wishlists/" + wishlist.getId() + "/" + bookId))
                .body(bookMapper.toDto(book));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeBookFromWishlist(
            @RequestHeader("User-Id") Long userId,
            @PathVariable Long bookId) {

        logger.info("Removing book {} from wishlist for user {}", bookId, userId);

        try {
            Wishlist wishlist = wishlistService.getWishlistByUserId(userId);
            wishlistService.removeBookFromWishlist(wishlist.getId(), bookId);
            logger.info("Book with id: {} removed from wishlist with id: {}", bookId, wishlist.getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Wishlist not found for user: {}", userId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user")
    public List<BookDTO> getUserWishlist(@RequestHeader("User-Id") Long userId) {

        logger.info("Getting wishlist for user {}", userId);

        try {
            Wishlist wishlist = wishlistService.getWishlistByUserId(userId);
            List<Book> books = wishlistService.getBooksByWishlistId(wishlist.getId());
            logger.info("Retrieved {} books from wishlist for user: {}", books.size(), userId);
            return bookMapper.toDtoList(books);
        } catch (Exception e) {
            logger.warn("No wishlist found for user: {}", userId);
            return List.of(); // Returnează listă goală
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createWishlistForUser(@RequestParam Long userId) {
        try {
            logger.info("Creating wishlist for user: {}", userId);

            Wishlist wishlist = wishlistService.createWishlist(userId);
            logger.info("Wishlist ensured for user: {} with ID: {}", userId, wishlist.getId());

            return ResponseEntity.ok("Wishlist created successfully");

        } catch (Exception e) {
            logger.error("Error creating wishlist for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create wishlist: " + e.getMessage());
        }
    }
}