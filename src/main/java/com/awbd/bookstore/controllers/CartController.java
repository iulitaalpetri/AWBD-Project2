package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.DTOs.CartDTO;
import com.awbd.bookstore.exceptions.token.InvalidTokenException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.CartMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.CartService;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private CartService cartService;
    private JwtUtil jwtUtil;
    private UserService userService;
    private CartMapper cartMapper;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private BookMapper bookMapper;

    public CartController(CartService cartService, JwtUtil jwtUtil, UserService userService, CartMapper cartMapper, BookMapper bookMapper){
        this.cartService = cartService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.cartMapper = cartMapper;
        this.bookMapper = bookMapper;
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<CartDTO> addBookToCart(
            @PathVariable Long bookId,
            @RequestHeader("Authorization") String authHeader) {
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


        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + user.getId()));

        cartService.addBookToCart(cart.getId(), bookId);
        logger.info("Book with ID {} added to cart with ID {}", bookId, cart.getId());

        return ResponseEntity.ok(cartMapper.toDto(cart));
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookDTO>> getCartBooks(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Missing or invalid authorization header");
            throw new InvalidTokenException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            logger.error("Invalid token");
            throw new InvalidTokenException("Invalid token");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));


        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + user.getId()));

        List<Book> booksInCart = cartService.getBooksInCart(cart.getId());

        logger.info("Retrieved books for cart with ID {}", cart.getId());

        return ResponseEntity.ok(bookMapper.toDtoList(booksInCart));
    }

    @GetMapping("/total")
    public ResponseEntity<Double> getTotalPrice(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Missing or invalid authorization header");
            throw new InvalidTokenException("Missing or invalid authorization header");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            logger.error("Invalid token");
            throw new InvalidTokenException("Invalid token");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + user.getId()));
        double totalPrice = cartService.calculateTotalPrice(cart.getId());

        logger.info("Total price for cart with ID {}: {}", cart.getId(), totalPrice);

        return ResponseEntity.ok(totalPrice);
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<?> removeBookFromCart(
            @PathVariable Long bookId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.error("Missing or invalid authorization header");
                throw new InvalidTokenException("Missing or invalid authorization header");
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);

            if (username == null) {
                logger.error("Invalid token");
                throw new InvalidTokenException("Invalid token");
            }

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

            Cart cart = cartService.getCartByUserId(user.getId()).get();
            cartService.removeBookFromCart(cart.getId(), bookId);
            logger.info("Book with ID {} removed from cart with ID {}", bookId, cart.getId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Book removed from cart successfully");
            return ResponseEntity.ok(response);

        } catch (InvalidTokenException | UserNotFoundException e) {
            logger.error("Authentication error: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            logger.error("Error removing book from cart: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to remove book from cart");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}