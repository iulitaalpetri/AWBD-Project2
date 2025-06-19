package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.order.OrderNotFoundException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.exceptions.cart.EmptyCartException;
import com.awbd.bookstore.exceptions.order.SaleNotFoundException;
import com.awbd.bookstore.models.*;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.OrderRepository;
import com.awbd.bookstore.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private BookRepository bookRepository;
    private SaleService saleService;
    private BookService bookService;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        BookRepository bookRepository, SaleService saleService, BookService bookService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.saleService = saleService;
        this.bookService = bookService;
    }
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Transactional
    public Order createOrder(Long userId, List<Long> bookIds, Long saleId) {
        logger.info("Creating order for user: {}, books: {}, sale: {}", userId, bookIds, saleId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found"));

        if (bookIds.isEmpty()) {
            throw new EmptyCartException("Cannot create order with empty book list");
        }

        Order order = new Order(user);

        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));

            if (!bookService.hasInStock(bookId, 1)) {
                int currentStock = bookService.getCurrentStock(bookId);
                logger.error("Insufficient stock for book '{}': current stock = {}", book.getTitle(), currentStock);
                throw new RuntimeException("Book '" + book.getTitle() + "' is out of stock (current stock: " + currentStock + ")");
            }

            order.addBook(book);
            logger.debug("Added book to order: {} (Current stock: {})", book.getTitle(), book.getStock());
        }

        Sale sale = null;
        if (saleId != null) {
            try {
                sale = saleService.getByIdWithStatusCheck(saleId);
                if (sale != null && sale.getIsActive()) {
                    order.setSale(sale);
                    logger.info("Applied sale: {} to order", sale.getSaleCode());
                } else {
                    logger.warn("Sale with ID {} is not active, proceeding without discount", saleId);
                    sale = null;
                }
            } catch (SaleNotFoundException e) {
                logger.warn("Sale with ID {} not found, proceeding without discount", saleId);
                sale = null;
            }
        } else {
            logger.info("No sale ID provided, creating order without discount");
        }

        double totalPrice = calculateOrderTotal(order.getBooks(), sale);
        order.setTotalPrice(totalPrice);

        Order savedOrder = orderRepository.save(order);
        logger.info("Order saved with ID: {} and total price: ${}", savedOrder.getId(), totalPrice);

        updateBookStockAfterOrder(savedOrder.getBooks());

        if (sale != null) {
            logger.info("Deactivating sale: {} after successful order", sale.getSaleCode());
            sale.setIsActive(false);
            saleService.update(sale.getId(), sale, null);
            logger.info("Sale {} has been deactivated", sale.getSaleCode());
        }

        return savedOrder;
    }

    private double calculateOrderTotal(Set<Book> books, Sale sale) {
        double totalPrice = 0.0;

        if (sale == null) {
            totalPrice = books.stream()
                    .mapToDouble(Book::getPrice)
                    .sum();
            logger.info("Order total without discount: ${}", totalPrice);
        } else {
            double discountPercentage = sale.getDiscountPercentage();
            List<Category> saleCategories = sale.getCategories();

            for (Book book : books) {
                double bookPrice = book.getPrice();
                if (saleCategories.contains(book.getCategory())) {
                    double discountedPrice = bookPrice * (1 - discountPercentage / 100);
                    totalPrice += discountedPrice;
                    logger.debug("Applied {}% discount to book: {} (${} -> ${})",
                            discountPercentage, book.getTitle(), bookPrice, discountedPrice);
                } else {
                    totalPrice += bookPrice;
                    logger.debug("No discount applied to book: {} (${})", book.getTitle(), bookPrice);
                }
            }
            logger.info("Order total with discount: ${}", totalPrice);
        }

        return totalPrice;
    }


    private void updateBookStockAfterOrder(Set<Book> books) {
        for (Book book : books) {
            try {
                int currentStock = bookService.getCurrentStock(book.getId());
                logger.debug("Current stock for book '{}': {}", book.getTitle(), currentStock);

                bookService.decreaseBookStock(book.getId(), 1);
                logger.info("Successfully decreased stock for book '{}' by 1", book.getTitle());

            } catch (Exception e) {
                logger.error("Failed to update stock for book '{}': {}", book.getTitle(), e.getMessage());
                throw new RuntimeException("Failed to update stock for book: " + book.getTitle(), e);
            }
        }
        logger.info("Stock updates completed for all {} books in order", books.size());
    }


    @Transactional
    public Order createOrderWithoutSale(Long userId, List<Long> bookIds) {
        return createOrder(userId, bookIds, null);
    }


    @Transactional
    public double calculateTotalPrice(Long orderId) {
        Order order = getOrderById(orderId);
        Sale sale = order.getSale();

        double newTotal = calculateOrderTotal(order.getBooks(), sale);

        order.setTotalPrice(newTotal);
        orderRepository.save(order);

        logger.info("Recalculated total for order {}: ${}", orderId, newTotal);
        return newTotal;
    }

    public List<Order> getUserOrderHistory(User user) {
        if (user == null) {
            throw new UserNotFoundException("User cannot be null");
        }
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));
    }

    public Order updateOrder(Long orderId, Long userId, Set<Long> bookIds) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        if (userId != null && !userId.equals(order.getUser().getId())) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found"));
            order.setUser(user);
        }

        if (bookIds == null || bookIds.isEmpty()) {
            throw new EmptyCartException("Book IDs cannot be empty");
        }

        Set<Book> books = new HashSet<>();
        for (Long bookId : bookIds) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));
            books.add(book);
        }
        order.setBooks(books);

        double newTotal = calculateOrderTotal(order.getBooks(), order.getSale());
        order.setTotalPrice(newTotal);

        return orderRepository.save(order);
    }

    public void deleteOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new OrderNotFoundException("Order with ID " + orderId + " not found");
        }
        orderRepository.deleteById(orderId);
        logger.info("Order with ID {} deleted successfully", orderId);
    }
}