package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.OrderRequestDTO;
import com.awbd.bookstore.DTOs.OrderDTO;
import com.awbd.bookstore.DTOs.UserDTO;
import com.awbd.bookstore.clients.AuthClient;
import com.awbd.bookstore.mappers.OrderMapper;
import com.awbd.bookstore.models.*;
import com.awbd.bookstore.services.CartService;
import com.awbd.bookstore.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final AuthClient authClient; // ADĂUGAT AuthClient
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    // CONSTRUCTOR MODIFICAT - adăugat AuthClient
    @Autowired
    public OrderController(OrderService orderService,
                           CartService cartService,
                           OrderMapper orderMapper,
                           AuthClient authClient) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.orderMapper = orderMapper;
        this.authClient = authClient;
    }

    // METODĂ HELPER pentru validarea utilizatorului
    private boolean validateUser(Long userId) {
        try {
            Map<String, Object> userValidation = authClient.validateUser(userId);
            Boolean isValid = (Boolean) userValidation.get("valid");
            if (isValid == null || !isValid) {
                logger.error("Invalid user: {}", userId);
                return false;
            }
            logger.info("User {} validated successfully", userId);
            return true;
        } catch (Exception e) {
            logger.error("Error validating user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @RequestHeader("User-Id") Long userId,
            @RequestBody(required = false) OrderRequestDTO request) {

        logger.info("=== ORDER CREATION STARTED ===");
        logger.info("User ID: {}", userId);

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Verifică dacă comanda aparține utilizatorului
            Order order = orderService.getOrderById(id);
            if (!order.getUserId().equals(userId)) {
                logger.error("User {} attempted to access order {} belonging to another user", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

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

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Order> orders = orderService.getUserOrderHistory(userId);
        if (orders.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        logger.info("Order history retrieved successfully for user: {}", userId);
        return ResponseEntity.ok(orderMapper.toDtoList(orders));
    }

    // ENDPOINT NOU: Obține istoric comenzi cu informații despre user
    @GetMapping("/history/with-user")
    public ResponseEntity<Map<String, Object>> getUserOrderHistoryWithUserInfo(
            @RequestHeader("User-Id") Long userId) {

        logger.info("Getting order history with user info for user: {}", userId);

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Obține informații despre user de la auth-service
            UserDTO userInfo = authClient.getUserInfo(userId);

            // Obține istoricul comenzilor
            List<Order> orders = orderService.getUserOrderHistory(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("user", userInfo);
            response.put("orders", orderMapper.toDtoList(orders));
            response.put("orderCount", orders.size());

            logger.info("Retrieved {} orders for user: {}", orders.size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting order history with user info for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders(
            @RequestHeader("User-Id") Long userId) {

        // VALIDARE PRIN AUTH-SERVICE - doar adminii pot vedea toate comenzile
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Verifică dacă utilizatorul este admin
            UserDTO user = authClient.getUserInfo(userId);
            if (!"ADMIN".equals(user.getRole())) {
                logger.error("Non-admin user {} attempted to access all orders", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Order> orders = orderService.getAllOrders();
            if (orders.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            logger.info("All orders retrieved successfully by admin: {}", userId);
            return ResponseEntity.ok(orderMapper.toDtoList(orders));
        } catch (Exception e) {
            logger.error("Error getting all orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDTO> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderDTO orderDTO,
            @RequestHeader("User-Id") Long userId) {

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Verifică dacă comanda aparține utilizatorului sau dacă este admin
            Order existingOrder = orderService.getOrderById(id);
            UserDTO user = authClient.getUserInfo(userId);

            if (!existingOrder.getUserId().equals(userId) && !"ADMIN".equals(user.getRole())) {
                logger.error("User {} attempted to update order {} without permission", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Order updatedOrder = orderService.updateOrder(id, orderDTO.getUserId(), orderDTO.getBookIds());
            OrderDTO updatedDTO = orderMapper.toDto(updatedOrder);
            logger.info("Order with ID {} updated successfully by user {}", id, userId);
            return ResponseEntity.ok(updatedDTO);
        } catch (Exception e) {
            logger.error("Error updating order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id,
            @RequestHeader("User-Id") Long userId) {

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // Verifică dacă comanda aparține utilizatorului sau dacă este admin
            Order order = orderService.getOrderById(id);
            UserDTO user = authClient.getUserInfo(userId);

            if (!order.getUserId().equals(userId) && !"ADMIN".equals(user.getRole())) {
                logger.error("User {} attempted to delete order {} without permission", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            orderService.deleteOrder(id);
            logger.info("Order with ID {} deleted by user {}", id, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @RequestHeader("User-Id") Long userId) {

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order order = orderService.getOrderById(id);

            // Verifică dacă comanda aparține utilizatorului sau dacă este admin
            UserDTO user = authClient.getUserInfo(userId);
            if (!order.getUserId().equals(userId) && !"ADMIN".equals(user.getRole())) {
                logger.error("User {} attempted to access order {} without permission", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            OrderDTO orderDTO = orderMapper.toDto(order);
            logger.info("Retrieved order with ID: {} for user: {}", id, userId);
            return ResponseEntity.ok(orderDTO);
        } catch (Exception e) {
            logger.error("Error getting order {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    // ENDPOINT NOU: Obține comandă cu informații despre user
    @GetMapping("/{id}/with-user")
    public ResponseEntity<Map<String, Object>> getOrderWithUser(
            @PathVariable Long id,
            @RequestHeader("User-Id") Long requestingUserId) {

        // VALIDARE PRIN AUTH-SERVICE
        if (!validateUser(requestingUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Order order = orderService.getOrderById(id);

            // Verifică dacă utilizatorul poate accesa această comandă
            UserDTO requestingUser = authClient.getUserInfo(requestingUserId);
            if (!order.getUserId().equals(requestingUserId) && !"ADMIN".equals(requestingUser.getRole())) {
                logger.error("User {} attempted to access order {} without permission", requestingUserId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Obține informații despre proprietarul comenzii
            UserDTO orderOwner = authClient.getUserInfo(order.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("order", orderMapper.toDto(order));
            response.put("orderOwner", orderOwner);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting order with user info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}