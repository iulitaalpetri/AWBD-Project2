package com.awbd.bookstore.controllers;

import com.awbd.bookstore.exceptions.token.InvalidTokenException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.WishlistMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.services.WishlistService;
import com.awbd.bookstore.utils.JwtUtil;
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
    private UserService userService;
    private JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(WishlistController.class);

    public WishlistController(WishlistService wishlistService,
                              WishlistMapper wishlistMapper,
                              BookMapper bookMapper,
                              UserService userService,
                              JwtUtil jwtUtil) {
        this.wishlistService = wishlistService;
        this.wishlistMapper = wishlistMapper;
        this.bookMapper = bookMapper;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }


    @PostMapping("/{bookId}")
    public ResponseEntity<BookDTO> addBookToWishlist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Missing or invalid authorization header");
            throw new InvalidTokenException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            logger.error("Invalid token: {}", token);
            throw new InvalidTokenException("Invalid token");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Long id = user.getWishlist().getId();
        Book book = wishlistService.addBookToWishlist(id, bookId);
        logger.info("Book with id: {} added to wishlist with id: {}", bookId, id);

        return ResponseEntity.created(URI.create("/api/wishlists/" + id + "/" + bookId))
                .body(bookMapper.toDto(book));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeBookFromWishlist(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Missing or invalid authorization header");
            throw new InvalidTokenException("Missing or invalid authorization header");
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        if (username == null) {
            logger.error("Invalid token: {}", token);
            throw new InvalidTokenException("Invalid token");
        }
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        Long id = user.getWishlist().getId();

        wishlistService.removeBookFromWishlist(id, bookId);
        logger.info("Book with id: {} removed from wishlist with id: {}", bookId, id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    public List<BookDTO> getUserWishlist(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header format");
            throw new InvalidTokenException("Invalid authorization header format");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            logger.error("Invalid token");
            throw new InvalidTokenException("Invalid token");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Wishlist wishlist = user.getWishlist();
        if (wishlist == null) {
            logger.warn("No wishlist found for user: {}", username);
            return List.of();
        }

        List<Book> books = wishlistService.getBooksByWishlistId(wishlist.getId());
        logger.info("Retrieved {} books from wishlist for user: {}", books.size(), username);

        return bookMapper.toDtoList(books);
    }
}