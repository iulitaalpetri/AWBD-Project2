package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.wishlist.BookAlreadyInWishlistException;
import com.awbd.bookstore.exceptions.wishlist.WishlistNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class WishlistService {

    private static final Logger logger = LoggerFactory.getLogger(WishlistService.class);

    private WishlistRepository wishlistRepository;
    private BookRepository bookRepository;

    public WishlistService(WishlistRepository wishlistRepository, BookRepository bookRepository) {
        this.wishlistRepository = wishlistRepository;
        this.bookRepository = bookRepository;
    }

    public List<Book> getBooksByWishlistId(Long wishlistId) {
        logger.debug("Retrieving books from wishlist ID: {}", wishlistId);

        if (!wishlistRepository.existsById(wishlistId)) {
            logger.warn("Wishlist not found when retrieving books - Wishlist ID: {}", wishlistId);
            throw new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found");
        }

        List<Book> books = wishlistRepository.findBooksByWishlistId(wishlistId);
        logger.info("Retrieved {} books from wishlist ID: {}", books.size(), wishlistId);

        // Log book titles for debugging
        if (logger.isDebugEnabled() && !books.isEmpty()) {
            books.forEach(book ->
                    logger.debug("Book in wishlist {}: '{}' (ID: {})", wishlistId, book.getTitle(), book.getId()));
        }

        return books;
    }

    public Book addBookToWishlist(Long wishlistId, Long bookId) {
        logger.info("Adding book ID: {} to wishlist ID: {}", bookId, wishlistId);

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> {
                    logger.warn("Wishlist not found when adding book - Wishlist ID: {}", wishlistId);
                    return new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found");
                });

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when adding to wishlist - Book ID: {}", bookId);
                    return new BookNotFoundException("Book with ID " + bookId + " not found");
                });

        Set<Book> books = wishlist.getBooks();
        int currentBookCount = books.size();

        for (Book existingBook : books) {
            if (existingBook.getId() == bookId) {
                logger.warn("Book '{}' (ID: {}) already exists in wishlist ID: {}",
                        book.getTitle(), bookId, wishlistId);
                throw new BookAlreadyInWishlistException("Book already exists in the wishlist");
            }
        }

        books.add(book);
        wishlist.setBooks(books);
        wishlistRepository.save(wishlist);

        logger.info("Book '{}' (ID: {}) successfully added to wishlist ID: {} (total books: {} -> {})",
                book.getTitle(), bookId, wishlistId, currentBookCount, books.size());

        return book;
    }

    public void removeBookFromWishlist(Long wishlistId, Long bookId) {
        logger.info("Removing book ID: {} from wishlist ID: {}", bookId, wishlistId);

        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> {
                    logger.warn("Wishlist not found when removing book - Wishlist ID: {}", wishlistId);
                    return new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found");
                });

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when removing from wishlist - Book ID: {}", bookId);
                    return new BookNotFoundException("Book with ID " + bookId + " not found");
                });

        int currentBookCount = wishlist.getBooks().size();
        boolean removed = wishlist.getBooks().removeIf(b -> b.getId() == bookId);

        if (!removed) {
            logger.warn("Book '{}' (ID: {}) not found in wishlist ID: {}",
                    book.getTitle(), bookId, wishlistId);
            throw new BookNotFoundException("Book with ID " + bookId + " not found in wishlist");
        }

        wishlistRepository.save(wishlist);

        logger.info("Book '{}' (ID: {}) successfully removed from wishlist ID: {} (total books: {} -> {})",
                book.getTitle(), bookId, wishlistId, currentBookCount, wishlist.getBooks().size());
    }

    public Wishlist getWishlistById(Long wishlistId) {
        logger.debug("Finding wishlist by ID: {}", wishlistId);

        return wishlistRepository.findById(wishlistId)
                .map(wishlist -> {
                    logger.debug("Wishlist found - ID: {}, User ID: {}, Books: {}",
                            wishlist.getId(), wishlist.getUserId(), wishlist.getBooks().size());
                    return wishlist;
                })
                .orElseThrow(() -> {
                    logger.warn("Wishlist not found with ID: {}", wishlistId);
                    return new WishlistNotFoundException("Wishlist with ID " + wishlistId + " not found");
                });
    }

    public Wishlist getWishlistByUserId(Long userId) {
        logger.debug("Finding wishlist for user ID: {}", userId);

        return wishlistRepository.findByUserId(userId)
                .map(wishlist -> {
                    logger.debug("Wishlist found for user ID: {} - Wishlist ID: {}, Books: {}",
                            userId, wishlist.getId(), wishlist.getBooks().size());
                    return wishlist;
                })
                .orElseThrow(() -> {
                    logger.warn("Wishlist not found for user ID: {}", userId);
                    return new WishlistNotFoundException("Wishlist for user with ID " + userId + " not found");
                });
    }

    public Wishlist createWishlist(Long userId) {
        logger.info("Creating wishlist for user ID: {}", userId);

        Optional<Wishlist> existing = wishlistRepository.findByUserId(userId);
        if (existing.isPresent()) {
            logger.info("Wishlist already exists for user ID: {}, returning existing wishlist ID: {}",
                    userId, existing.get().getId());
            return existing.get();
        }

        Wishlist wishlist = new Wishlist(userId);
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        logger.info("Wishlist successfully created for user ID: {} - Wishlist ID: {}",
                userId, savedWishlist.getId());

        return savedWishlist;
    }
}