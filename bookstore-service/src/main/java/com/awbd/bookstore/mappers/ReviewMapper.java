package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.ReviewDTO;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.repositories.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReviewMapper {
    private final BookRepository bookRepository;

    // CONSTRUCTOR MODIFICAT - eliminat UserRepository
    @Autowired
    public ReviewMapper(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public ReviewDTO toDto(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setContent(review.getContent());
        dto.setRating(review.getRating());

        // MODIFICAT: folosim direct userId
        dto.setUserId(review.getUserId());

        // NOTE: Username nu mai e disponibil direct - ar trebui să chemi auth-service
        // Pentru acum lăsăm null sau setăm un placeholder
        dto.setUsername("User-" + review.getUserId()); // Temporary - ar trebui API call la auth-service

        if (review.getBook() != null) {
            dto.setBookId(review.getBook().getId());
        }

        return dto;
    }

    public Review toEntity(ReviewDTO dto) {
        Review review = new Review();
        review.setContent(dto.getContent());
        review.setRating(dto.getRating());

        // MODIFICAT: setăm direct userId
        if (dto.getUserId() != null) {
            review.setUserId(dto.getUserId());
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

        // MODIFICAT: setăm direct userId
        if (dto.getUserId() != null) {
            review.setUserId(dto.getUserId());
        }

        if (dto.getBookId() != null) {
            Book book = bookRepository.findById(dto.getBookId())
                    .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + dto.getBookId()));
            review.setBook(book);
        }
    }
}