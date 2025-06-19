package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.repositories.CategoryRepository;
import com.awbd.bookstore.repositories.AuthorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookMapper {
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

    @Autowired
    public BookMapper(CategoryRepository categoryRepository, AuthorRepository authorRepository) {
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
    }

    public BookDTO toDto(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setPrice(book.getPrice());
        dto.setStock(book.getStock());

        if (book.getCategory() != null) {
            dto.setCategoryId(book.getCategory().getId());
        }

        if (book.getAuthor() != null) {
            dto.setAuthorId(book.getAuthor().getId());
            dto.setAuthor(book.getAuthor().getName());
        }

        return dto;
    }

    public Book toEntity(BookDTO dto) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setPrice(dto.getPrice());
        book.setStock(dto.getStock());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + dto.getCategoryId()));
            book.setCategory(category);
        }

        if (dto.getAuthorId() != null) {
            Author author = authorRepository.findById(dto.getAuthorId())
                    .orElseThrow(() -> new EntityNotFoundException("Author not found with id: " + dto.getAuthorId()));
            book.setAuthor(author);
        }

        return book;
    }

    public List<BookDTO> toDtoList(List<Book> books) {
        return books.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(BookDTO dto, Book book) {
        book.setTitle(dto.getTitle());
        book.setPrice(dto.getPrice());
        book.setStock(dto.getStock());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + dto.getCategoryId()));
            book.setCategory(category);
        }

        if (dto.getAuthorId() != null) {
            Author author = authorRepository.findById(dto.getAuthorId())
                    .orElseThrow(() -> new EntityNotFoundException("Author not found with id: " + dto.getAuthorId()));
            book.setAuthor(author);
        }
    }
}