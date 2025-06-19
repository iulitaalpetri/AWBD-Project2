package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.OrderRequestDTO;
import com.awbd.bookstore.DTOs.OrderDTO;
import com.awbd.bookstore.annotations.RequireAdmin;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.OrderMapper;
import com.awbd.bookstore.models.*;
import com.awbd.bookstore.services.CartService;
import com.awbd.bookstore.services.OrderService;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    public OrderController(OrderService orderService,
                           JwtUtil jwtUtil,
                           UserService userService,
                           CartService cartService,
                           OrderMapper orderMapper) {
        this.orderService = orderService;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.cartService = cartService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) OrderRequestDTO request) {

        logger.info("=== ORDER CREATION STARTED ===");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        if (username == null) {
            logger.error("Invalid token - could not extract username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        logger.info("Username from token: {}", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        logger.info("User found: {} (ID: {})", user.getUsername(), user.getId());

        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + user.getId()));
        logger.info("Cart found: ID {}", cart.getId());

        List<Book> booksInCart = cartService.getBooksInCart(cart.getId());
        logger.info("Books in cart: {}", booksInCart.size());

        if (booksInCart.isEmpty()) {
            logger.error("Cart is empty for user: {}", username);
            return ResponseEntity.badRequest().build();
        }

        List<Long> bookIds = booksInCart.stream()
                .map(Book::getId)
                .collect(Collectors.toList());
        logger.info("Book IDs: {}", bookIds);

        Long saleId = null;
        if (request != null && request.getSaleId() != null) {
            saleId = request.getSaleId();
            logger.info("Sale ID from request: {}", saleId);
        } else {
            logger.info("No sale ID in request, proceeding without sale");
        }

        try {
            logger.info("About to call orderService.createOrder with userId: {}, bookIds: {}, saleId: {}",
                    user.getId(), bookIds, saleId);
            Order order = orderService.createOrder(user.getId(), bookIds, saleId);
            logger.info("Order created successfully: ID {}", order.getId());

            logger.info("About to clear cart with ID: {}", cart.getId());
            cartService.clearCart(cart.getId());
            logger.info("Cart cleared for user: {}", username);

            logger.info("About to map order to DTO");
            OrderDTO orderDTO = orderMapper.toDto(order);
            logger.info("Order creation completed successfully");
            return ResponseEntity.ok(orderDTO);

        } catch (Exception e) {
            logger.error("=== DETAILED ERROR INFO ===");
            logger.error("Error class: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            logger.error("Error cause: {}", e.getCause() != null ? e.getCause().toString() : "No cause");
            logger.error("Full stack trace:", e);

            throw new RuntimeException("Order creation failed: " + e.getMessage(), e);
        }
    }

    @PostMapping("/no-sale")
    public ResponseEntity<OrderDTO> createOrderWithoutSale(
            @RequestHeader("Authorization") String authHeader) {

        logger.info("=== ORDER CREATION WITHOUT SALE STARTED ===");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        if (username == null) {
            logger.error("Invalid token - could not extract username");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Cart cart = cartService.getCartByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + user.getId()));

        List<Book> booksInCart = cartService.getBooksInCart(cart.getId());
        if (booksInCart.isEmpty()) {
            logger.error("Cart is empty for user: {}", username);
            return ResponseEntity.badRequest().build();
        }

        List<Long> bookIds = booksInCart.stream()
                .map(Book::getId)
                .collect(Collectors.toList());

        try {
            Order order = orderService.createOrderWithoutSale(user.getId(), bookIds);
            logger.info("Order created without sale: ID {}", order.getId());

            cartService.clearCart(cart.getId());
            logger.info("Cart cleared for user: {}", username);

            OrderDTO orderDTO = orderMapper.toDto(order);
            return ResponseEntity.ok(orderDTO);

        } catch (Exception e) {
            logger.error("Error creating order without sale:", e);
            throw new RuntimeException("Order creation failed: " + e.getMessage(), e);
        }
    }

    @PostMapping("/{id}/calculate-total")
    public ResponseEntity<Double> calculateOrderTotal(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        logger.info("Calculating total for order: {}", id);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            double total = orderService.calculateTotalPrice(id);
            logger.info("Calculated total for order {}: ${}", id, total);
            return ResponseEntity.ok(total);
        } catch (Exception e) {
            logger.error("Error calculating order total for order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<OrderDTO>> getUserOrderHistory(
            @RequestHeader("Authorization") String authHeader) {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        List<Order> orders = orderService.getUserOrderHistory(user);
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        logger.info("Order history retrieved successfully for user: {}", username);
        return ResponseEntity.ok(orderMapper.toDtoList(orders));
    }

    @GetMapping
    @RequireAdmin
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        logger.info("All orders retrieved successfully");
        return ResponseEntity.ok(orderMapper.toDtoList(orders));
    }

    @PutMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @RequestBody OrderDTO orderDTO) {
        Order updatedOrder = orderService.updateOrder(id, orderDTO.getUserId(), orderDTO.getBookIds());
        OrderDTO updatedDTO = orderMapper.toDto(updatedOrder);
        logger.info("Order with ID {} updated successfully", id);
        return ResponseEntity.ok(updatedDTO);
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        logger.info("Order with ID {} deleted", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Order order = orderService.getOrderById(id);
        OrderDTO orderDTO = orderMapper.toDto(order);
        logger.info("Retrieved order with ID: {}", id);
        return ResponseEntity.ok(orderDTO);
    }
}