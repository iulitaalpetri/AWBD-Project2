package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.AuthorDTO;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.services.AuthorService;
import com.awbd.bookstore.mappers.AuthorMapper;
import com.awbd.bookstore.annotations.RequireAdmin;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    private AuthorService authorService;
    private AuthorMapper authorMapper;
    private static final Logger logger = LoggerFactory.getLogger(AuthorController.class);

    public AuthorController(AuthorService authorService, AuthorMapper authorMapper) {
        this.authorService = authorService;
        this.authorMapper = authorMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Author> createAuthor(
            @RequestBody
            @Valid
            AuthorDTO authorDTO) {
        logger.info("Creating new author: {}", authorDTO.getName());
        Author author = authorMapper.toEntity(authorDTO);
        Author createdAuthor = authorService.create(author);
        logger.info("Created new author: {}", authorDTO.getName());
        return ResponseEntity.created(URI.create("/api/authors/" + createdAuthor.getId()))
                .body(createdAuthor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorDTO> getAuthorById(@PathVariable Long id) {
        Author author = authorService.getById(id);
        return ResponseEntity.ok(authorMapper.toDto(author));
    }

    @GetMapping
    public List<AuthorDTO> getAllAuthors() {
        return authorService.getAll()
                .stream()
                .map(authorMapper::toDto)
                .toList();
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Author> updateAuthor(
            @PathVariable
            Long id,

            @RequestBody
            @Valid
            AuthorDTO authorDTO) {
        if (authorDTO.getId() != null && !id.equals(authorDTO.getId())) {
            throw new RuntimeException("Id from path does not match with id from request");
        }

        Author author = authorMapper.toEntity(authorDTO);
        logger.info("Updating author with id: {}", id);
        return ResponseEntity.ok()
                .body(authorService.update(id, author));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        authorService.delete(id);
        logger.info("Deleted author with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}