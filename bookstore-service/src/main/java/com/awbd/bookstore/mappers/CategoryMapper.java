package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.CategoryDTO;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.models.Sale;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.SaleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {
    private final BookRepository bookRepository;
    private final SaleRepository saleRepository;

    @Autowired
    public CategoryMapper(BookRepository bookRepository, SaleRepository saleRepository) {
        this.bookRepository = bookRepository;
        this.saleRepository = saleRepository;
    }

    public CategoryDTO toDto(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());

        if (category.getBooks() != null && !category.getBooks().isEmpty()) {
            List<Long> bookIds = category.getBooks().stream()
                    .map(Book::getId)
                    .collect(Collectors.toList());
            dto.setBookIds(bookIds);
        }

        if (category.getSales() != null && !category.getSales().isEmpty()) {
            List<Long> saleIds = category.getSales().stream()
                    .map(Sale::getId)
                    .collect(Collectors.toList());
            dto.setSaleIds(saleIds);
        }

        return dto;
    }

    public Category toEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        return category;
    }

    public List<CategoryDTO> toDtoList(List<Category> categories) {
        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(CategoryDTO dto, Category category) {
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
    }
}