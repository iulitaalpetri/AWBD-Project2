package com.awbd.bookstore.repositories;

import com.awbd.bookstore.models.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    List<Author> findByName(String name);

    List<Author> findByNameContaining(String nameFragment);

    boolean existsByName(String name);

    boolean existsByNameAndBirthDate(String name, LocalDate birthDate);

}