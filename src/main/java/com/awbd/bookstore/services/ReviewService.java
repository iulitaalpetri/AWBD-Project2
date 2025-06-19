package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.review.ReviewNotFoundException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.ReviewRepository;
import com.awbd.bookstore.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    private ReviewRepository reviewRepository;
    private UserRepository userRepository;
    private BookRepository bookRepository;

    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository, BookRepository bookRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    // add review
    public Review addReview(Review review, Long bookId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Book with ID " + bookId + " not found."));

        review.setUser(user);
        review.setBook(book);

        return reviewRepository.save(review);
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Book with ID " + bookId + " not found.");
        }
        return reviewRepository.findByBookId(bookId);
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