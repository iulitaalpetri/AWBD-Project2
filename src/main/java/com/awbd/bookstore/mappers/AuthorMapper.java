package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.AuthorDTO;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.repositories.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthorMapper {
    private final BookRepository bookRepository;

    @Autowired
    public AuthorMapper(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public AuthorDTO toDto(Author author) {
        AuthorDTO dto = new AuthorDTO();
        dto.setId(author.getId());
        dto.setName(author.getName());
        dto.setBiography(author.getBiography());
        dto.setBirthDate(author.getBirthDate());

        if (author.getBooks() != null && !author.getBooks().isEmpty()) {
            List<Long> bookIds = author.getBooks().stream()
                    .map(Book::getId)
                    .collect(Collectors.toList());
            dto.setBookIds(bookIds);
        }

        return dto;
    }

    public Author toEntity(AuthorDTO dto) {
        Author author = new Author();
        author.setName(dto.getName());
        author.setBiography(dto.getBiography());
        author.setBirthDate(dto.getBirthDate());


        if (dto.getBookIds() != null && !dto.getBookIds().isEmpty()) {
            List<Book> books = dto.getBookIds().stream()
                    .map(id -> bookRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id)))
                    .collect(Collectors.toList());

            books.forEach(author::addBook);
        }

        return author;
    }

    public List<AuthorDTO> toDtoList(List<Author> authors) {
        return authors.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(AuthorDTO dto, Author author) {
        author.setName(dto.getName());
        author.setBiography(dto.getBiography());
        author.setBirthDate(dto.getBirthDate());

        if (dto.getBookIds() != null && !dto.getBookIds().isEmpty()) {

            List<Book> newBooks = dto.getBookIds().stream()
                    .map(id -> bookRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id)))
                    .collect(Collectors.toList());

            if (author.getBooks() != null) {
                List<Book> currentBooks = new ArrayList<>(author.getBooks());
                currentBooks.forEach(author::removeBook);
            }

            newBooks.forEach(author::addBook);
        }
    }
}