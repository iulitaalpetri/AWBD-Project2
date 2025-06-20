package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.review.ReviewNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    private ReviewRepository reviewRepository;
    private BookRepository bookRepository;

    // CONSTRUCTOR MODIFICAT - eliminat UserRepository
    public ReviewService(ReviewRepository reviewRepository, BookRepository bookRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
    }

    // METODĂ MODIFICATĂ - folosim userId direct
    public Review addReview(Review review, Long bookId, Long userId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found."));

        // MODIFICAT: setăm userId direct în loc să căutăm User-ul
        review.setUserId(userId);
        review.setBook(book);

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Book with ID " + bookId + " not found.");
        }
        return reviewRepository.findByBookId(bookId);
    }

    // METODĂ NOUĂ: obține review-urile unui user
    public List<Review> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserId(userId);
    }

    public Review getReviewById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException("Review with ID " + id + " not found."));
    }

    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFoundException("Review with ID " + id + " not found.");
        }
        reviewRepository.deleteById(id);
    }


}