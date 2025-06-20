package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.ReviewDTO;
import com.awbd.bookstore.mappers.ReviewMapper;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.services.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private ReviewService reviewService;
    private ReviewMapper reviewMapper;
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    // CONSTRUCTOR MODIFICAT - doar ce ai tu
    public ReviewController(ReviewService reviewService, ReviewMapper reviewMapper) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<ReviewDTO> createReview(
            @PathVariable Long bookId,
            @RequestBody @Valid ReviewDTO reviewDTO,
            @RequestHeader("User-Id") Long userId) {

        Review review = reviewMapper.toEntity(reviewDTO);
        Review savedReview = reviewService.addReview(review, bookId, userId);

        logger.info("Created review for book ID: {} by user: {}", bookId, userId);

        return ResponseEntity.created(URI.create("/api/reviews/" + savedReview.getId()))
                .body(reviewMapper.toDto(savedReview));
    }

    @GetMapping("/book/{bookId}")
    public List<ReviewDTO> getReviewsByBookId(@PathVariable Long bookId) {
        List<Review> reviews = reviewService.getReviewsByBookId(bookId);
        logger.info("Fetched {} reviews for book ID: {}", reviews.size(), bookId);
        return reviewMapper.toDtoList(reviews);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @RequestHeader("User-Id") Long userId) {

        Review review = reviewService.getReviewById(id);
        if (!review.getUserId().equals(userId)) {
            logger.error("User {} attempted to delete review {} belonging to another user", userId, id);
            throw new RuntimeException("Not authorized to delete this review");
        }

        reviewService.deleteReview(id);
        logger.info("Review with ID {} deleted by user: {}", id, userId);

        return ResponseEntity.noContent().build();
    }
}