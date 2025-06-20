package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.OrderRequestDTO;
import com.awbd.bookstore.DTOs.OrderDTO;
import com.awbd.bookstore.mappers.OrderMapper;
import com.awbd.bookstore.models.*;
import com.awbd.bookstore.services.CartService;
import com.awbd.bookstore.services.OrderService;
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
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    // CONSTRUCTOR MODIFICAT - eliminat JwtUtil și UserService
    @Autowired
    public OrderController(OrderService orderService,
                           CartService cartService,
                           OrderMapper orderMapper) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestHeader("User-Id") Long userId,
            @RequestBody(required = false) OrderRequestDTO request) {

        logger.info("=== ORDER CREATION STARTED ===");
        logger.info("User ID: {}", userId);

        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        logger.info("Cart found: ID {}", cart.getId());

        List<Book> booksInCart = cartService.getBooksInCart(cart.getId());
        logger.info("Books in cart: {}", booksInCart.size());

        if (booksInCart.isEmpty()) {
            logger.error("Cart is empty for user: {}", userId);
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
                    userId, bookIds, saleId);
            Order order = orderService.createOrder(userId, bookIds, saleId);
            logger.info("Order created successfully: ID {}", order.getId());

            logger.info("About to clear cart with ID: {}", cart.getId());
            cartService.clearCart(cart.getId());
            logger.info("Cart cleared for user: {}", userId);

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
            @RequestHeader("User-Id") Long userId) {

        logger.info("=== ORDER CREATION WITHOUT SALE STARTED ===");
        logger.info("User ID: {}", userId);

        Cart cart = cartService.getCartByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        List<Book> booksInCart = cartService.getBooksInCart(cart.getId());
        if (booksInCart.isEmpty()) {
            logger.error("Cart is empty for user: {}", userId);
            return ResponseEntity.badRequest().build();
        }

        List<Long> bookIds = booksInCart.stream()
                .map(Book::getId)
                .collect(Collectors.toList());

        try {
            Order order = orderService.createOrderWithoutSale(userId, bookIds);
            logger.info("Order created without sale: ID {}", order.getId());

            cartService.clearCart(cart.getId());
            logger.info("Cart cleared for user: {}", userId);

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
            @RequestHeader("User-Id") Long userId) {

        logger.info("Calculating total for order: {}", id);

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
            @RequestHeader("User-Id") Long userId) {

        logger.info("Getting order history for user: {}", userId);

        List<Order> orders = orderService.getUserOrderHistory(userId);
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        logger.info("Order history retrieved successfully for user: {}", userId);
        return ResponseEntity.ok(orderMapper.toDtoList(orders));
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        logger.info("All orders retrieved successfully");
        return ResponseEntity.ok(orderMapper.toDtoList(orders));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @RequestBody OrderDTO orderDTO) {
        Order updatedOrder = orderService.updateOrder(id, orderDTO.getUserId(), orderDTO.getBookIds());
        OrderDTO updatedDTO = orderMapper.toDto(updatedOrder);
        logger.info("Order with ID {} updated successfully", id);
        return ResponseEntity.ok(updatedDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        logger.info("Order with ID {} deleted", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @RequestHeader("User-Id") Long userId) {

        Order order = orderService.getOrderById(id);
        OrderDTO orderDTO = orderMapper.toDto(order);
        logger.info("Retrieved order with ID: {}", id);
        return ResponseEntity.ok(orderDTO);
    }
}