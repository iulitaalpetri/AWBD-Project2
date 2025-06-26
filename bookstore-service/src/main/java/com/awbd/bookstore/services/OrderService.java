package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.order.OrderNotFoundException;
import com.awbd.bookstore.exceptions.cart.EmptyCartException;
import com.awbd.bookstore.exceptions.order.SaleNotFoundException;
import com.awbd.bookstore.models.*;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private OrderRepository orderRepository;
    private BookRepository bookRepository;
    private SaleService saleService;
    private BookService bookService;

    public OrderService(OrderRepository orderRepository, BookRepository bookRepository,
                        SaleService saleService, BookService bookService) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.saleService = saleService;
        this.bookService = bookService;
    }

    @Transactional
    public Order createOrder(Long userId, List<Long> bookIds, Long saleId) {
        logger.info("Creating order for user ID: {}, books: {}, sale ID: {}", userId, bookIds, saleId);

        if (bookIds.isEmpty()) {
            logger.warn("Order creation failed - empty book list for user ID: {}", userId);
            throw new EmptyCartException("Cannot create order with empty book list");
        }

        Order order = new Order(userId);
        logger.debug("Initialized new order for user ID: {}", userId);

        for (Long bookId : bookIds) {
            logger.debug("Processing book ID: {} for order", bookId);

            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> {
                        logger.warn("Book not found when creating order - Book ID: {}", bookId);
                        return new BookNotFoundException("Book with ID " + bookId + " not found");
                    });

            if (!bookService.hasInStock(bookId, 1)) {
                int currentStock = bookService.getCurrentStock(bookId);
                logger.error("Insufficient stock for book '{}' (ID: {}): current stock = {}",
                        book.getTitle(), bookId, currentStock);
                throw new RuntimeException("Book '" + book.getTitle() + "' is out of stock (current stock: " + currentStock + ")");
            }

            order.addBook(book);
            logger.debug("Added book '{}' (ID: {}) to order (Current stock: {})",
                    book.getTitle(), bookId, book.getStock());
        }

        Sale sale = null;
        if (saleId != null) {
            logger.debug("Processing sale ID: {} for order", saleId);
            try {
                sale = saleService.getByIdWithStatusCheck(saleId);
                if (sale != null && sale.getIsActive()) {
                    order.setSale(sale);
                    logger.info("Applied active sale '{}' (ID: {}) to order", sale.getSaleCode(), saleId);
                } else {
                    logger.warn("Sale '{}' (ID: {}) is not active, proceeding without discount",
                            sale != null ? sale.getSaleCode() : "null", saleId);
                    sale = null;
                }
            } catch (SaleNotFoundException e) {
                logger.warn("Sale with ID {} not found, proceeding without discount: {}", saleId, e.getMessage());
                sale = null;
            }
        } else {
            logger.debug("No sale ID provided, creating order without discount");
        }

        double totalPrice = calculateOrderTotal(order.getBooks(), sale);
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);
        logger.info("Order successfully saved with ID: {} for user: {} with total price: ${}",
                savedOrder.getId(), userId, totalPrice);

        updateBookStockAfterOrder(savedOrder.getBooks());

        if (sale != null) {
            logger.info("Deactivating sale '{}' (ID: {}) after successful order",
                    sale.getSaleCode(), sale.getId());
            sale.setIsActive(false);
            saleService.update(sale.getId(), sale, null);
            logger.info("Sale '{}' has been deactivated", sale.getSaleCode());
        }

        logger.info("Order creation completed successfully - Order ID: {}, User ID: {}, Books: {}",
                savedOrder.getId(), userId, bookIds.size());

        return savedOrder;
    }

    private double calculateOrderTotal(Set<Book> books, Sale sale) {
        logger.debug("Calculating order total for {} books with sale: {}",
                books.size(), sale != null ? sale.getSaleCode() : "none");

        double totalPrice = 0.0;

        if (sale == null) {
            totalPrice = books.stream()
                    .mapToDouble(Book::getPrice)
                    .sum();
            logger.info("Order total calculated without discount: ${}", totalPrice);
        } else {
            double discountPercentage = sale.getDiscountPercentage();
            List<Category> saleCategories = sale.getCategories();

            logger.debug("Applying {}% discount for sale '{}' to applicable categories",
                    discountPercentage, sale.getSaleCode());

            for (Book book : books) {
                double bookPrice = book.getPrice();
                if (saleCategories.contains(book.getCategory())) {
                    double discountedPrice = bookPrice * (1 - discountPercentage / 100);
                    totalPrice += discountedPrice;
                    logger.debug("Applied {}% discount to book '{}' (ID: {}): ${} -> ${}",
                            discountPercentage, book.getTitle(), book.getId(), bookPrice, discountedPrice);
                } else {
                    totalPrice += bookPrice;
                    logger.debug("No discount applied to book '{}' (ID: {}): ${} (category not in sale)",
                            book.getTitle(), book.getId(), bookPrice);
                }
            }
            logger.info("Order total calculated with discount from sale '{}': ${}",
                    sale.getSaleCode(), totalPrice);
        }

        return totalPrice;
    }

    private void updateBookStockAfterOrder(Set<Book> books) {
        logger.info("Updating stock for {} books after order completion", books.size());

        for (Book book : books) {
            try {
                int currentStock = bookService.getCurrentStock(book.getId());
                logger.debug("Current stock for book '{}' (ID: {}): {}",
                        book.getTitle(), book.getId(), currentStock);

                bookService.decreaseBookStock(book.getId(), 1);
                logger.info("Successfully decreased stock for book '{}' (ID: {}) by 1",
                        book.getTitle(), book.getId());

            } catch (Exception e) {
                logger.error("Failed to update stock for book '{}' (ID: {}): {}",
                        book.getTitle(), book.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to update stock for book: " + book.getTitle(), e);
            }
        }
        logger.info("Stock updates completed successfully for all {} books in order", books.size());
    }

    @Transactional
    public Order createOrderWithoutSale(Long userId, List<Long> bookIds) {
        logger.info("Creating order without sale for user ID: {} with {} books", userId, bookIds.size());
        return createOrder(userId, bookIds, null);
    }

    @Transactional
    public double calculateTotalPrice(Long orderId) {
        logger.debug("Recalculating total price for order ID: {}", orderId);

        Order order = getOrderById(orderId);
        Sale sale = order.getSale();

        double oldTotal = order.getTotalPrice();
        double newTotal = calculateOrderTotal(order.getBooks(), sale);

        order.setTotalPrice(newTotal);
        orderRepository.save(order);

        logger.info("Recalculated total for order ID: {}: ${} -> ${}", orderId, oldTotal, newTotal);
        return newTotal;
    }

    public List<Order> getUserOrderHistory(Long userId) {
        logger.debug("Retrieving order history for user ID: {}", userId);

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        logger.info("Retrieved {} orders for user ID: {}", orders.size(), userId);

        return orders;
    }

    public List<Order> getAllOrders() {
        logger.debug("Retrieving all orders");

        List<Order> orders = orderRepository.findAllByOrderByOrderDateDesc();
        logger.info("Retrieved {} total orders", orders.size());

        return orders;
    }

    public Order getOrderById(Long orderId) {
        logger.debug("Finding order by ID: {}", orderId);

        return orderRepository.findById(orderId)
                .map(order -> {
                    logger.debug("Order found - ID: {}, User ID: {}, Total: ${}",
                            order.getId(), order.getUserId(), order.getTotalPrice());
                    return order;
                })
                .orElseThrow(() -> {
                    logger.warn("Order not found with ID: {}", orderId);
                    return new OrderNotFoundException("Order with ID " + orderId + " not found");
                });
    }

    public Order updateOrder(Long orderId, Long userId, Set<Long> bookIds) {
        logger.info("Updating order ID: {} for user ID: {} with {} books", orderId, userId, bookIds.size());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    logger.warn("Order not found when updating - Order ID: {}", orderId);
                    return new OrderNotFoundException("Order with ID " + orderId + " not found");
                });

        Long originalUserId = order.getUserId();

        if (userId != null && !userId.equals(order.getUserId())) {
            order.setUserId(userId);
            logger.debug("Updated user ID for order {}: {} -> {}", orderId, originalUserId, userId);
        }

        if (bookIds == null || bookIds.isEmpty()) {
            logger.warn("Order update failed - empty book IDs for order ID: {}", orderId);
            throw new EmptyCartException("Book IDs cannot be empty");
        }

        Set<Book> books = new HashSet<>();
        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> {
                        logger.warn("Book not found when updating order - Book ID: {}", bookId);
                        return new BookNotFoundException("Book with ID " + bookId + " not found");
                    });
            books.add(book);
        }

        int oldBookCount = order.getBooks().size();
        order.setBooks(books);
        logger.debug("Updated books for order ID: {}: {} -> {} books", orderId, oldBookCount, books.size());

        double oldTotal = order.getTotalPrice();
        double newTotal = calculateOrderTotal(order.getBooks(), order.getSale());
        order.setTotalPrice(newTotal);

        Order savedOrder = orderRepository.save(order);
        logger.info("Order successfully updated - ID: {}, Books: {} -> {}, Total: ${} -> ${}",
                orderId, oldBookCount, books.size(), oldTotal, newTotal);

        return savedOrder;
    }

    public void deleteOrder(Long orderId) {
        logger.info("Deleting order with ID: {}", orderId);

        if (!orderRepository.existsById(orderId)) {
            logger.warn("Order deletion failed - order not found with ID: {}", orderId);
            throw new OrderNotFoundException("Order with ID " + orderId + " not found");
        }

        // Log order details before deletion for audit
        Order orderToDelete = orderRepository.findById(orderId).orElse(null);
        if (orderToDelete != null) {
            logger.debug("Deleting order - ID: {}, User ID: {}, Books: {}, Total: ${}",
                    orderId, orderToDelete.getUserId(), orderToDelete.getBooks().size(),
                    orderToDelete.getTotalPrice());
        }

        orderRepository.deleteById(orderId);
        logger.info("Order with ID {} deleted successfully", orderId);
    }
}