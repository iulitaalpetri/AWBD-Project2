package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // gasire review dupa carte
    List<Review> findByBookId(Long bookId);
    // review dupa user
    List<Review> findByUserId(Long userId);

}