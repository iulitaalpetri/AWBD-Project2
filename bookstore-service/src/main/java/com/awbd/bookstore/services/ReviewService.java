package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.book.BookNotFoundException;
import com.awbd.bookstore.exceptions.review.ReviewNotFoundException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private ReviewRepository reviewRepository;
    private BookRepository bookRepository;

    public ReviewService(ReviewRepository reviewRepository, BookRepository bookRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
    }

    public Review addReview(Review review, Long bookId, Long userId) {
        logger.info("Adding review for book ID: {} by user ID: {} with rating: {}",
                bookId, userId, review.getRating());

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> {
                    logger.warn("Book not found when adding review - Book ID: {}", bookId);
                    return new BookNotFoundException("Book with ID " + bookId + " not found.");
                });

        review.setUserId(userId);
        review.setBook(book);

        logger.debug("Review details - Book: '{}', User ID: {}, Rating: {}, Content length: {}",
                book.getTitle(), userId, review.getRating(),
                review.getContent() != null ? review.getContent().length() : 0);

        Review savedReview = reviewRepository.save(review);

        logger.info("Review successfully added - ID: {}, Book: '{}' (ID: {}), User ID: {}, Rating: {}",
                savedReview.getId(), book.getTitle(), bookId, userId, savedReview.getRating());

        return savedReview;
    }

    public List<Review> getReviewsByBookId(Long bookId) {
        logger.debug("Retrieving reviews for book ID: {}", bookId);

        if (!bookRepository.existsById(bookId)) {
            logger.warn("Book not found when retrieving reviews - Book ID: {}", bookId);
            throw new BookNotFoundException("Book with ID " + bookId + " not found.");
        }

        List<Review> reviews = reviewRepository.findByBookId(bookId);
        logger.info("Found {} reviews for book ID: {}", reviews.size(), bookId);

        // Log rating distribution for debugging
        if (logger.isDebugEnabled() && !reviews.isEmpty()) {
            double averageRating = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
            logger.debug("Book ID: {} - Average rating: {:.2f} from {} reviews",
                    bookId, averageRating, reviews.size());
        }

        return reviews;
    }

    public List<Review> getReviewsByUserId(Long userId) {
        logger.debug("Retrieving reviews for user ID: {}", userId);

        List<Review> reviews = reviewRepository.findByUserId(userId);
        logger.info("Found {} reviews for user ID: {}", reviews.size(), userId);

        // Log user's review activity for debugging
        if (logger.isDebugEnabled() && !reviews.isEmpty()) {
            double averageUserRating = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
            logger.debug("User ID: {} - Average rating given: {:.2f} across {} reviews",
                    userId, averageUserRating, reviews.size());
        }

        return reviews;
    }

    public Review getReviewById(Long id) {
        logger.debug("Finding review by ID: {}", id);

        return reviewRepository.findById(id)
                .map(review -> {
                    logger.debug("Review found - ID: {}, Book ID: {}, User ID: {}, Rating: {}",
                            review.getId(),
                            review.getBook() != null ? review.getBook().getId() : "null",
                            review.getUserId(),
                            review.getRating());
                    return review;
                })
                .orElseThrow(() -> {
                    logger.warn("Review not found with ID: {}", id);
                    return new ReviewNotFoundException("Review with ID " + id + " not found.");
                });
    }

    public void deleteReview(Long id) {
        logger.info("Deleting review with ID: {}", id);

        if (!reviewRepository.existsById(id)) {
            logger.warn("Review deletion failed - review not found with ID: {}", id);
            throw new ReviewNotFoundException("Review with ID " + id + " not found.");
        }

        // Log review details before deletion for audit
        Review reviewToDelete = reviewRepository.findById(id).orElse(null);
        if (reviewToDelete != null) {
            logger.debug("Deleting review - ID: {}, Book: '{}' (ID: {}), User ID: {}, Rating: {}",
                    id,
                    reviewToDelete.getBook() != null ? reviewToDelete.getBook().getTitle() : "null",
                    reviewToDelete.getBook() != null ? reviewToDelete.getBook().getId() : "null",
                    reviewToDelete.getUserId(),
                    reviewToDelete.getRating());
        }

        reviewRepository.deleteById(id);
        logger.info("Review with ID {} deleted successfully", id);
    }
}