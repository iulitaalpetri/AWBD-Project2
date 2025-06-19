package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.book.OutOfStockException;
import com.awbd.bookstore.exceptions.cart.BookAlreadyInCartException;
import com.awbd.bookstore.exceptions.cart.CartNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.CartRepository;
import com.awbd.bookstore.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    private CartRepository cartRepository;
    private BookRepository bookRepository;
    private UserRepository userRepository;
    private SaleService saleService;

    public CartService(CartRepository cartRepository, BookRepository bookRepository,
                       UserRepository userRepository, SaleService saleService) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.saleService = saleService;
    }

    public Optional<Cart> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId);
    }

//    public Cart createCart(Long userId) {
//        if (cartRepository.existsByUserId(userId)) {
//            throw new CartAlreadyExistsException("Cart already exists for this user");
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found"));
//
//        Cart cart = new Cart(user);
//        return cartRepository.save(cart);
//    }

    public void addBookToCart(Long cartId, Long bookId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart with ID " + cartId + " not found"));

        if (cartRepository.existsBookInCart(cartId, bookId)) {
            throw new BookAlreadyInCartException("Book already in Cart");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));

        if (book.getStock() <= 0) {
            throw new OutOfStockException("Book is out of stock.");
        }

        cart.addBook(book);
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long cartId) {
        System.out.println("=== CartService.clearCart CALLED ===");
        System.out.println("cartId: " + cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart with ID " + cartId + " not found"));

        System.out.println("Books in cart before clearing: " + cart.getBooks().size());
        cart.getBooks().clear();
        cartRepository.save(cart);
        System.out.println("Cart cleared and saved");
    }

    public double calculateTotalPrice(Long cartId) {
        if (!cartRepository.existsById(cartId)) {
            throw new CartNotFoundException("Cart with ID " + cartId + " not found");
        }
        return cartRepository.calculateTotalPrice(cartId);
    }


    public List<Book> getBooksInCart(Long cartId) {
        if (!cartRepository.existsById(cartId)) {
            throw new CartNotFoundException("Cart with ID " + cartId + " not found");
        }
        return cartRepository.findBooksInCart(cartId);
    }

    @Transactional
    public void removeBookFromCart(Long cartId, Long bookId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart with ID " + cartId + " not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));

        if (!cart.getBooks().contains(book)) {
            throw new BookNotFoundException("Book is not in the cart");
        }

        cart.removeBook(book);
        cartRepository.save(cart);
    }
}