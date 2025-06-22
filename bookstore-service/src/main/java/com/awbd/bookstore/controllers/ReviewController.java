package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.ReviewDTO;
import com.awbd.bookstore.DTOs.UserDTO;
import com.awbd.bookstore.clients.AuthClient;
import com.awbd.bookstore.mappers.ReviewMapper;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.services.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private ReviewService reviewService;
    private ReviewMapper reviewMapper;
    private AuthClient authClient;
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    // CONSTRUCTOR MODIFICAT - adăugat AuthClient
    public ReviewController(ReviewService reviewService, ReviewMapper reviewMapper, AuthClient authClient) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
        this.authClient = authClient;
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<ReviewDTO> createReview(
            @PathVariable Long bookId,
            @RequestBody @Valid ReviewDTO reviewDTO,
            @RequestHeader("User-Id") Long userId) {

        logger.info("Creating review for book {} by user {}", bookId, userId);

        // VALIDARE PRIN AUTH-SERVICE
        try {
            Map<String, Object> userValidation = authClient.validateUser(userId);
            Boolean isValid = (Boolean) userValidation.get("valid");
            if (isValid == null || !isValid) {
                logger.error("Invalid user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            logger.info("User {} validated successfully", userId);
        } catch (Exception e) {
            logger.error("Error validating user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

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

        logger.info("User {} attempting to delete review {}", userId, id);

        // VALIDARE PRIN AUTH-SERVICE
        try {
            Map<String, Object> userValidation = authClient.validateUser(userId);
            Boolean isValid = (Boolean) userValidation.get("valid");
            if (isValid == null || !isValid) {
                logger.error("Invalid user: {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            logger.error("Error validating user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        Review review = reviewService.getReviewById(id);
        if (!review.getUserId().equals(userId)) {
            logger.error("User {} attempted to delete review {} belonging to another user", userId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        reviewService.deleteReview(id);
        logger.info("Review with ID {} deleted by user: {}", id, userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long id) {
        Review review = reviewService.getReviewById(id);
        logger.info("Retrieved review with ID: {}", id);
        return ResponseEntity.ok(reviewMapper.toDto(review));
    }

    // ENDPOINT NOU: Obține review-urile unui user cu informațiile despre user
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserReviews(@PathVariable Long userId) {
        try {
            // Obține informații despre user de la auth-service
            UserDTO userInfo = authClient.getUserInfo(userId);

            // Obține review-urile user-ului
            List<Review> reviews = reviewService.getReviewsByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("user", userInfo);
            response.put("reviews", reviewMapper.toDtoList(reviews));
            response.put("reviewCount", reviews.size());

            logger.info("Retrieved {} reviews for user: {}", reviews.size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting reviews for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    // ENDPOINT NOU: Obține review cu informații despre autor
    @GetMapping("/{id}/with-author")
    public ResponseEntity<Map<String, Object>> getReviewWithAuthor(@PathVariable Long id) {
        try {
            Review review = reviewService.getReviewById(id);

            // Obține informații despre autorul review-ului
            UserDTO authorInfo = authClient.getUserInfo(review.getUserId());

            Map<String, Object> response = new HashMap<>();
            response.put("review", reviewMapper.toDto(review));
            response.put("author", authorInfo);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting review with author info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}