package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.wishlist.BookAlreadyInWishlistException;
import com.awbd.bookstore.exceptions.wishlist.WishlistNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class WishlistService {
    private WishlistRepository wishlistRepository;
    private BookRepository bookRepository;

    public WishlistService(WishlistRepository wishlistRepository, BookRepository bookRepository) {
        this.wishlistRepository = wishlistRepository;
        this.bookRepository = bookRepository;
    }

    public List<Book> getBooksByWishlistId(Long wishlistId) {
        if (!wishlistRepository.existsById(wishlistId)) {
            throw new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found");
        }
        return wishlistRepository.findBooksByWishlistId(wishlistId);
    }

    public Book addBookToWishlist(Long wishlistId, Long bookId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));

        Set<Book> books = wishlist.getBooks();
        for (Book existingBook : books) {
            if (existingBook.getId() == bookId) {
                throw new BookAlreadyInWishlistException("Book already exists in the wishlist");
            }
        }

        books.add(book);
        wishlist.setBooks(books);
        wishlistRepository.save(wishlist);

        return book;
    }

    public void removeBookFromWishlist(Long wishlistId, Long bookId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found"));

        boolean removed = wishlist.getBooks().removeIf(b -> b.getId() == bookId);

        if (!removed) {
            throw new BookNotFoundException("Book with ID " + bookId + " not found in wishlist");
        }

        wishlistRepository.save(wishlist);
    }

    public Wishlist getWishlistById(Long wishlistId) {
        return wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found"));
    }

    public Wishlist getWishlistByUserId(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new WishlistNotFoundException("Wishlist for user with ID " + userId + " not found"));
    }
}