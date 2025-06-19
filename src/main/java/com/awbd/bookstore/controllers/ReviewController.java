package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.ReviewDTO;
import com.awbd.bookstore.exceptions.token.InvalidTokenException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.ReviewMapper;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.ReviewService;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.utils.JwtUtil;
import org.springframework.http.HttpStatus;
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
    private UserService userService;
    private JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    public ReviewController(ReviewService reviewService, ReviewMapper reviewMapper,
                            UserService userService, JwtUtil jwtUtil) {
        this.reviewService = reviewService;
        this.reviewMapper = reviewMapper;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/book/{bookId}")
    public ResponseEntity<ReviewDTO> createReview(
            @PathVariable Long bookId,
            @RequestBody @Valid ReviewDTO reviewDTO,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header format");
            throw new InvalidTokenException("Invalid authorization header format");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            logger.error("Invalid token");
            throw new InvalidTokenException("Invalid token");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        Review review = reviewMapper.toEntity(reviewDTO);
        Review savedReview = reviewService.addReview(review, bookId, user.getId());

        logger.info("Created review for book ID: {} by user: {}", bookId, username);

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
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header format");
            throw new InvalidTokenException("Invalid authorization header format");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);

        if (username == null) {
            logger.error("Invalid token");
            throw new InvalidTokenException("Invalid token");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));


        Review review = reviewService.getReviewById(id);
        if (review.getUser().getId() != user.getId() && !user.getRole().equals(User.Role.ADMIN)) {
            logger.error("User {} attempted to delete review {} belonging to another user", username, id);
            throw new RuntimeException("Not authorized to delete this review");
        }

        reviewService.deleteReview(id);
        logger.info("Review with ID {} deleted by user: {}", id, username);

        return ResponseEntity.noContent().build();
    }
}