package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.book.OutOfStockException;
import com.awbd.bookstore.exceptions.cart.BookAlreadyInCartException;
import com.awbd.bookstore.exceptions.cart.CartNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private CartRepository cartRepository;
    private BookRepository bookRepository;
    private SaleService saleService;

    public CartService(CartRepository cartRepository, BookRepository bookRepository,
                       SaleService saleService) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.saleService = saleService;
    }

    public Optional<Cart> getCartByUserId(Long userId) {
        logger.debug("Finding cart for user ID: {}", userId);

        Optional<Cart> cart = cartRepository.findByUserId(userId);

        if (cart.isPresent()) {
            logger.debug("Cart found for user ID: {} - Cart ID: {}", userId, cart.get().getId());
        } else {
            logger.debug("No cart found for user ID: {}", userId);
        }

        return cart;
    }

    public Cart createCart(Long userId) {
        logger.info("Creating cart for user ID: {}", userId);

        if (cartRepository.existsByUserId(userId)) {
            logger.info("Cart already exists for user ID: {}, returning existing cart", userId);
            Cart existingCart = cartRepository.findByUserId(userId).get();
            logger.debug("Returning existing cart ID: {} for user: {}", existingCart.getId(), userId);
            return existingCart;
        }

        Cart cart = new Cart(userId);
        Cart savedCart = cartRepository.save(cart);

        logger.info("Cart successfully created for user ID: {} - Cart ID: {}", userId, savedCart.getId());
        return savedCart;
    }

    public void addBookToCart(Long cartId, Long bookId) {
        logger.info("Adding book ID: {} to cart ID: {}", bookId, cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> {
                    logger.warn("Cart not found when adding book - Cart ID: {}", cartId);
                    return new CartNotFoundException("Cart with ID " + cartId + " not found");
                });

        if (cartRepository.existsBookInCart(cartId, bookId)) {
            logger.warn("Book ID: {} already exists in cart ID: {}", bookId, cartId);
            throw new BookAlreadyInCartException("Book already in Cart");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when adding to cart - Book ID: {}", bookId);
                    return new BookNotFoundException("Book with ID " + bookId + " not found");
                });

        if (book.getStock() <= 0) {
            logger.warn("Attempted to add out-of-stock book to cart - Book: '{}' (ID: {}), Stock: {}",
                    book.getTitle(), bookId, book.getStock());
            throw new OutOfStockException("Book is out of stock.");
        }

        cart.addBook(book);
        cartRepository.save(cart);

        logger.info("Book '{}' (ID: {}) successfully added to cart ID: {}",
                book.getTitle(), bookId, cartId);
    }

    @Transactional
    public void clearCart(Long cartId) {
        logger.info("Clearing cart ID: {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> {
                    logger.warn("Cart not found when clearing - Cart ID: {}", cartId);
                    return new CartNotFoundException("Cart with ID " + cartId + " not found");
                });

        int booksCount = cart.getBooks().size();
        logger.debug("Cart ID: {} contains {} books before clearing", cartId, booksCount);

        cart.getBooks().clear();
        cartRepository.save(cart);

        logger.info("Cart ID: {} successfully cleared ({} books removed)", cartId, booksCount);
    }

    public double calculateTotalPrice(Long cartId) {
        logger.debug("Calculating total price for cart ID: {}", cartId);

        if (!cartRepository.existsById(cartId)) {
            logger.warn("Cart not found when calculating total price - Cart ID: {}", cartId);
            throw new CartNotFoundException("Cart with ID " + cartId + " not found");
        }

        double totalPrice = cartRepository.calculateTotalPrice(cartId);
        logger.debug("Total price calculated for cart ID: {} = ${}", cartId, totalPrice);

        return totalPrice;
    }

    public List<Book> getBooksInCart(Long cartId) {
        logger.debug("Retrieving books from cart ID: {}", cartId);

        if (!cartRepository.existsById(cartId)) {
            logger.warn("Cart not found when retrieving books - Cart ID: {}", cartId);
            throw new CartNotFoundException("Cart with ID " + cartId + " not found");
        }

        List<Book> books = cartRepository.findBooksInCart(cartId);
        logger.info("Retrieved {} books from cart ID: {}", books.size(), cartId);

        return books;
    }

    @Transactional
    public void removeBookFromCart(Long cartId, Long bookId) {
        logger.info("Removing book ID: {} from cart ID: {}", bookId, cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> {
                    logger.warn("Cart not found when removing book - Cart ID: {}", cartId);
                    return new CartNotFoundException("Cart with ID " + cartId + " not found");
                });

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when removing from cart - Book ID: {}", bookId);
                    return new BookNotFoundException("Book with ID " + bookId + " not found");
                });

        if (!cart.getBooks().contains(book)) {
            logger.warn("Book '{}' (ID: {}) is not in cart ID: {}", book.getTitle(), bookId, cartId);
            throw new BookNotFoundException("Book is not in the cart");
        }

        cart.removeBook(book);
        cartRepository.save(cart);

        logger.info("Book '{}' (ID: {}) successfully removed from cart ID: {}",
                book.getTitle(), bookId, cartId);
    }
}