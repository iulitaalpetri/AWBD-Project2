package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.author.AuthorAlreadyExistsException;
import com.awbd.bookstore.exceptions.author.AuthorNotFoundException;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.repositories.AuthorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthorService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorService.class);

    private AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Author create(Author author) {
        logger.info("Creating author with name: {} and birth date: {}",
                author.getName(), author.getBirthDate());

        if (authorRepository.existsByNameAndBirthDate(author.getName(), author.getBirthDate())) {
            logger.warn("Author creation failed - author already exists: {} ({})",
                    author.getName(), author.getBirthDate());
            throw new AuthorAlreadyExistsException("Author already exists");
        }

        Author savedAuthor = authorRepository.save(author);
        logger.info("Author successfully created with ID: {} - {}",
                savedAuthor.getId(), savedAuthor.getName());

        return savedAuthor;
    }

    public Author getById(Long id) {
        logger.debug("Finding author by ID: {}", id);

        return authorRepository.findById(id)
                .map(author -> {
                    logger.debug("Author found - ID: {}, Name: {}", author.getId(), author.getName());
                    return author;
                })
                .orElseThrow(() -> {
                    logger.warn("Author not found with ID: {}", id);
                    return new AuthorNotFoundException("Author with ID " + id + " not found");
                });
    }

    public List<Author> getAll() {
        logger.debug("Retrieving all authors");

        List<Author> authors = authorRepository.findAll();
        logger.info("Retrieved {} authors", authors.size());

        return authors;
    }

    public Author update(Long id, Author updatedAuthor) {
        logger.info("Updating author with ID: {} - New name: {}", id, updatedAuthor.getName());

        return authorRepository.findById(id)
                .map(existingAuthor -> {
                    String oldName = existingAuthor.getName();

                    existingAuthor.setName(updatedAuthor.getName());
                    existingAuthor.setBiography(updatedAuthor.getBiography());
                    existingAuthor.setBirthDate(updatedAuthor.getBirthDate());

                    Author savedAuthor = authorRepository.save(existingAuthor);
                    logger.info("Author successfully updated - ID: {}, Old name: {}, New name: {}",
                            id, oldName, savedAuthor.getName());

                    return savedAuthor;
                })
                .orElseThrow(() -> {
                    logger.warn("Author update failed - author not found with ID: {}", id);
                    return new AuthorNotFoundException("Author with ID " + id + " not found");
                });
    }

    public void delete(Long id) {
        logger.info("Deleting author with ID: {}", id);

        if(!authorRepository.existsById(id)) {
            logger.warn("Author deletion failed - author not found with ID: {}", id);
            throw new AuthorNotFoundException("Author with ID " + id + " not found");
        }

        authorRepository.deleteById(id);
        logger.info("Author successfully deleted with ID: {}", id);
    }
}