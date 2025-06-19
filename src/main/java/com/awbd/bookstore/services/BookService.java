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
        return bookRepository.findAll();
    }

    public List<Book> searchByTitle(String title) {
        return bookRepository.findByTitleContaining(title);
    }

    public Book addBook(Book book) {
        return bookRepository.save(book);
    }

    /**
     * Update book stock to a specific quantity (existing method)
     */
    public void updateBookStock(Long bookId, int quantity) {
        bookRepository.updateStock(bookId, quantity);
    }

    /**
     * Set book stock to a specific value
     */
    @Transactional
    public void setBookStock(Long bookId, int newStock) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

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
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

        int currentStock = book.getStock();
        int newStock = currentStock - quantity;

        if (newStock < 0) {
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
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));

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
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));
        return book.getStock() >= quantity;
    }

    /**
     * Get current stock for a book
     */
    public int getCurrentStock(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));
        return book.getStock();
    }

    public List<Book> getBooksInStock() {
        return bookRepository.findByStockGreaterThan(0);
    }

    public List<Book> getBookBycategoryId(Long CategoryId) {
        return bookRepository.findByCategoryId(CategoryId);
    }

    public void deleteBook(Long bookId) {
        bookRepository.deleteById(bookId);
    }

    public List<BookDTO> getBooksByAuthorId(Long authorId) {
        List<Book> books = bookRepository.findByAuthorId(authorId);
        return books.stream()
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
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id));
    }

    public Book updateBook(Long id, BookDTO bookDto) {
        return bookRepository.findById(id)
                .map(existingBook -> {
                    existingBook.setTitle(bookDto.getTitle());
                    existingBook.setPrice(bookDto.getPrice());
                    existingBook.setStock(bookDto.getStock());

                    bookMapper.updateEntityFromDto(bookDto, existingBook);

                    return bookRepository.save(existingBook);
                })
                .orElseThrow(() -> new EntityNotFoundException("Book with ID " + id + " not found"));
    }
}