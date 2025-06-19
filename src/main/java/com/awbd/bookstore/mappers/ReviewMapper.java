package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.ReviewDTO;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReviewMapper {
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewMapper(BookRepository bookRepository, UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public ReviewDTO toDto(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setContent(review.getContent());
        dto.setRating(review.getRating());


        if (review.getUser() != null) {
            dto.setUserId(review.getUser().getId());
            dto.setUsername(review.getUser().getUsername());
        }

        if (review.getBook() != null) {
            dto.setBookId(review.getBook().getId());
        }

        return dto;
    }

    public Review toEntity(ReviewDTO dto) {
        Review review = new Review();
        review.setContent(dto.getContent());
        review.setRating(dto.getRating());


        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + dto.getUserId()));
            review.setUser(user);
        }

        if (dto.getBookId() != null) {
            Book book = bookRepository.findById(dto.getBookId())
                    .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + dto.getBookId()));
            review.setBook(book);
        }

        return review;
    }

    public List<ReviewDTO> toDtoList(List<Review> reviews) {
        return reviews.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(ReviewDTO dto, Review review) {
        review.setContent(dto.getContent());
        review.setRating(dto.getRating());


        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + dto.getUserId()));
            review.setUser(user);
        }

        if (dto.getBookId() != null) {
            Book book = bookRepository.findById(dto.getBookId())
                    .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + dto.getBookId()));
            review.setBook(book);
        }
    }
}