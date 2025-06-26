package com.awbd.bookstore.services;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.repositories.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BookService {
    private BookRepository bookRepository;
    private BookMapper bookMapper;
    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    public BookService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public List<Book> getAllBooks() {
        logger.debug("Retrieving all books");
        List<Book> books = bookRepository.findAll();
        logger.info("Retrieved {} books", books.size());
        return books;
    }

    public List<Book> searchByTitle(String title) {
        logger.debug("Searching books by title containing: '{}'", title);
        List<Book> books = bookRepository.findByTitleContaining(title);
        logger.info("Found {} books matching title: '{}'", books.size(), title);
        return books;
    }

    public Book addBook(Book book) {
        logger.info("Adding new book: '{}' by author ID: {}", book.getTitle(),
                book.getAuthor() != null ? book.getAuthor().getId() : "null");

        Book savedBook = bookRepository.save(book);
        logger.info("Book successfully added with ID: {} - '{}'", savedBook.getId(), savedBook.getTitle());

        return savedBook;
    }

    /**
     * Update book stock to a specific quantity (existing method)
     */
    public void updateBookStock(Long bookId, int quantity) {
        logger.info("Updating stock for book ID: {} to quantity: {}", bookId, quantity);

        try {
            bookRepository.updateStock(bookId, quantity);
            logger.info("Stock updated successfully for book ID: {} to {}", bookId, quantity);
        } catch (Exception e) {
            logger.error("Failed to update stock for book ID: {} to quantity: {}", bookId, quantity, e);
            throw e;
        }
    }

    /**
     * Set book stock to a specific value
     */
    @Transactional
    public void setBookStock(Long bookId, int newStock) {
        logger.debug("Setting stock for book ID: {} to {}", bookId, newStock);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when setting stock - ID: {}", bookId);
                    return new EntityNotFoundException("Book not found with id: " + bookId);
                });

        int oldStock = book.getStock();
        book.setStock(newStock);
        bookRepository.save(book);

        logger.info("Stock updated for book '{}' (ID: {}): {} -> {}",
                book.getTitle(), bookId, oldStock, newStock);
    }

    /**
     * Decrease book stock by a specific amount
     */
    @Transactional
    public void decreaseBookStock(Long bookId, int quantity) {
        logger.debug("Decreasing stock for book ID: {} by {}", bookId, quantity);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when decreasing stock - ID: {}", bookId);
                    return new EntityNotFoundException("Book not found with id: " + bookId);
                });

        int currentStock = book.getStock();
        int newStock = currentStock - quantity;

        if (newStock < 0) {
            logger.warn("Insufficient stock for book '{}' (ID: {}). Current: {}, requested: {}",
                    book.getTitle(), bookId, currentStock, quantity);
            throw new RuntimeException("Insufficient stock for book '" + book.getTitle() +
                    "'. Current stock: " + currentStock + ", requested: " + quantity);
        }

        book.setStock(newStock);
        bookRepository.save(book);

        logger.info("Decreased stock for book '{}' (ID: {}): {} -> {} (decreased by {})",
                book.getTitle(), bookId, currentStock, newStock, quantity);
    }

    /**
     * Increase book stock by a specific amount
     */
    @Transactional
    public void increaseBookStock(Long bookId, int quantity) {
        logger.debug("Increasing stock for book ID: {} by {}", bookId, quantity);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when increasing stock - ID: {}", bookId);
                    return new EntityNotFoundException("Book not found with id: " + bookId);
                });

        int currentStock = book.getStock();
        int newStock = currentStock + quantity;

        book.setStock(newStock);
        bookRepository.save(book);

        logger.info("Increased stock for book '{}' (ID: {}): {} -> {} (increased by {})",
                book.getTitle(), bookId, currentStock, newStock, quantity);
    }

    /**
     * Check if book has sufficient stock
     */
    public boolean hasInStock(Long bookId, int quantity) {
        logger.debug("Checking stock availability for book ID: {} - required quantity: {}", bookId, quantity);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when checking stock - ID: {}", bookId);
                    return new EntityNotFoundException("Book not found with id: " + bookId);
                });

        boolean hasStock = book.getStock() >= quantity;
        logger.debug("Stock check for book '{}' (ID: {}): has {} in stock, required {}, available: {}",
                book.getTitle(), bookId, book.getStock(), quantity, hasStock);

        return hasStock;
    }

    /**
     * Get current stock for a book
     */
    public int getCurrentStock(Long bookId) {
        logger.debug("Getting current stock for book ID: {}", bookId);

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when getting stock - ID: {}", bookId);
                    return new EntityNotFoundException("Book not found with id: " + bookId);
                });

        int stock = book.getStock();
        logger.debug("Current stock for book '{}' (ID: {}): {}", book.getTitle(), bookId, stock);

        return stock;
    }

    public List<Book> getBooksInStock() {
        logger.debug("Retrieving books with stock > 0");
        List<Book> books = bookRepository.findByStockGreaterThan(0);
        logger.info("Found {} books in stock", books.size());
        return books;
    }

    public List<Book> getBookBycategoryId(Long categoryId) {
        logger.debug("Retrieving books by category ID: {}", categoryId);
        List<Book> books = bookRepository.findByCategoryId(categoryId);
        logger.info("Found {} books for category ID: {}", books.size(), categoryId);
        return books;
    }

    public void deleteBook(Long bookId) {
        logger.info("Deleting book with ID: {}", bookId);

        // Check if book exists before deletion
        if (!bookRepository.existsById(bookId)) {
            logger.warn("Book deletion failed - book not found with ID: {}", bookId);
            throw new EntityNotFoundException("Book not found with id: " + bookId);
        }

        bookRepository.deleteById(bookId);
        logger.info("Book successfully deleted with ID: {}", bookId);
    }

    public List<BookDTO> getBooksByAuthorId(Long authorId) {
        logger.debug("Retrieving books by author ID: {}", authorId);

        List<Book> books = bookRepository.findByAuthorId(authorId);
        List<BookDTO> bookDTOs = books.stream()
                .map(book -> new BookDTO(
                        book.getId(),
                        book.getTitle(),
                        book.getPrice(),
                        book.getStock(),
                        book.getCategory().getId(),
                        book.getAuthor().getId(),
                        book.getAuthor().getName()
                ))
                .collect(Collectors.toList());

        logger.info("Found {} books for author ID: {}", bookDTOs.size(), authorId);
        return bookDTOs;
    }

    public Book getBookById(Long id) {
        logger.debug("Finding book by ID: {}", id);

        return bookRepository.findById(id)
                .map(book -> {
                    logger.debug("Book found - ID: {}, Title: '{}'", book.getId(), book.getTitle());
                    return book;
                })
                .orElseThrow(() -> {
                    logger.warn("Book not found with ID: {}", id);
                    return new EntityNotFoundException("Book not found with id: " + id);
                });
    }

    public Book updateBook(Long id, BookDTO bookDto) {
        logger.info("Updating book with ID: {} - New title: '{}'", id, bookDto.getTitle());

        return bookRepository.findById(id)
                .map(existingBook -> {
                    String oldTitle = existingBook.getTitle();
                    Double oldPrice = existingBook.getPrice();
                    Integer oldStock = existingBook.getStock();

                    existingBook.setTitle(bookDto.getTitle());
                    existingBook.setPrice(bookDto.getPrice());
                    existingBook.setStock(bookDto.getStock());

                    bookMapper.updateEntityFromDto(bookDto, existingBook);

                    Book savedBook = bookRepository.save(existingBook);

                    logger.info("Book successfully updated - ID: {}, Title: '{}' -> '{}', Price: {} -> {}, Stock: {} -> {}",
                            id, oldTitle, savedBook.getTitle(), oldPrice, savedBook.getPrice(),
                            oldStock, savedBook.getStock());

                    return savedBook;
                })
                .orElseThrow(() -> {
                    logger.warn("Book update failed - book not found with ID: {}", id);
                    return new EntityNotFoundException("Book with ID " + id + " not found");
                });
    }
}