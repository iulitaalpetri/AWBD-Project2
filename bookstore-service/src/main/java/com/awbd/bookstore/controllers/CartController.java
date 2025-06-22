package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.DTOs.CartDTO;
import com.awbd.bookstore.DTOs.UserDTO;
import com.awbd.bookstore.clients.AuthClient;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.CartMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.services.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private CartService cartService;
    private CartMapper cartMapper;
    private BookMapper bookMapper;
    private AuthClient authClient;
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    public CartController(CartService cartService, CartMapper cartMapper, BookMapper bookMapper, AuthClient authClient){
        this.cartService = cartService;
        this.cartMapper = cartMapper;
        this.bookMapper = bookMapper;
        this.authClient = authClient;
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<CartDTO> addBookToCart(
            @PathVariable Long bookId,
            @RequestHeader("User-Id") Long userId) {

        logger.info("Adding book {} to cart for user {}", bookId, userId);

        // VALIDARE PRIN AUTH-SERVICE
        try {
            Map<String, Object> userValidation = authClient.validateUser(userId);
            Boolean isValid = (Boolean) userValidation.get("valid");
            if (isValid == null || !isValid) {
                logger.error("Invalid user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            logger.info("User {} validated successfully", userId);
        } catch (Exception e) {
            logger.error("Error validating user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        Cart cart = cartService.getCartByUserId(userId)
                .orElseGet(() -> cartService.createCart(userId));

        cartService.addBookToCart(cart.getId(), bookId);
        logger.info("Book with ID {} added to cart with ID {}", bookId, cart.getId());

        return ResponseEntity.ok(cartMapper.toDto(cart));
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookDTO>> getCartBooks(@RequestHeader("User-Id") Long userId) {
        logger.info("Getting cart books for user {}", userId);

        // VALIDARE PRIN AUTH-SERVICE
        try {
            Map<String, Object> userValidation = authClient.validateUser(userId);
            Boolean isValid = (Boolean) userValidation.get("valid");
            if (isValid == null || !isValid) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            logger.error("Error validating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        List<Book> booksInCart = cartService.getBooksInCart(cart.getId());
        logger.info("Retrieved {} books for cart with ID {}", booksInCart.size(), cart.getId());

        return ResponseEntity.ok(bookMapper.toDtoList(booksInCart));
    }

    @GetMapping("/total")
    public ResponseEntity<Double> getTotalPrice(@RequestHeader("User-Id") Long userId) {
        logger.info("Getting total price for user {}", userId);

        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        double totalPrice = cartService.calculateTotalPrice(cart.getId());
        logger.info("Total price for cart with ID {}: {}", cart.getId(), totalPrice);

        return ResponseEntity.ok(totalPrice);
    }

    @DeleteMapping("/remove/{bookId}")
    public ResponseEntity<?> removeBookFromCart(
            @PathVariable Long bookId,
            @RequestHeader("User-Id") Long userId) {

        try {
            logger.info("Removing book {} from cart for user {}", bookId, userId);

            Cart cart = cartService.getCartByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

            cartService.removeBookFromCart(cart.getId(), bookId);
            logger.info("Book with ID {} removed from cart with ID {}", bookId, cart.getId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Book removed from cart successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error removing book from cart: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to remove book from cart");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createCartForUser(@RequestParam Long userId) {
        try {
            logger.info("Creating cart for user: {}", userId);

            if (cartService.getCartByUserId(userId).isPresent()) {
                logger.info("Cart already exists for user: {}", userId);
                return ResponseEntity.ok("Cart already exists");
            }

            Cart cart = cartService.createCart(userId);
            logger.info("Cart created with ID: {} for user: {}", cart.getId(), userId);

            return ResponseEntity.ok("Cart created successfully");

        } catch (Exception e) {
            logger.error("Error creating cart for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create cart: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart(@RequestHeader("User-Id") Long userId) {
        logger.info("Getting cart for user {}", userId);

        Cart cart = cartService.getCartByUserId(userId)
                .orElseGet(() -> cartService.createCart(userId));

        return ResponseEntity.ok(cartMapper.toDto(cart));
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getCartWithUserInfo(@RequestHeader("User-Id") Long userId) {
        try {
            UserDTO userInfo = authClient.getUserInfo(userId);

            Cart cart = cartService.getCartByUserId(userId)
                    .orElseGet(() -> cartService.createCart(userId));

            Map<String, Object> response = new HashMap<>();
            response.put("cart", cartMapper.toDto(cart));
            response.put("user", userInfo);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting cart with user info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}