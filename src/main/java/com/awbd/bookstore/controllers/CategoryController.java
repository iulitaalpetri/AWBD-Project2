package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.DTOs.CategoryDTO;
import com.awbd.bookstore.annotations.RequireAdmin;
import com.awbd.bookstore.exceptions.category.CategoryNotFoundException;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.CategoryMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.repositories.CategoryRepository;
import com.awbd.bookstore.services.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private CategoryService categoryService;
    private CategoryMapper categoryMapper;
    private BookMapper bookMapper;
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper, BookMapper bookMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
        this.bookMapper = bookMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Category> addCategory(
            @RequestBody
            @Valid
            CategoryDTO categoryDto) {
        Category category = categoryMapper.toEntity(categoryDto);
        Category savedCategory = categoryService.createCategory(category);
        logger.info("Category created: {}", savedCategory);

        return ResponseEntity.created(URI.create("/api/categories/" + savedCategory.getId()))
                .body(savedCategory);
    }

    @GetMapping("/{id}/books")
    public List<BookDTO> getBooksInCategory(@PathVariable Long id) {
        List<Book> books = categoryService.getBooksInCategory(id);
        logger.info("Retrieved {} books from category with id: {}", books.size(), id);
        return bookMapper.toDtoList(books);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        logger.info("Category with id {} deleted", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        logger.info("Retrieved {} categories", categories.size());
        return categoryMapper.toDtoList(categories);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable
            Long id,

            @RequestBody
            @Valid
            CategoryDTO categoryDto) {

        if (categoryDto.getId() != null && !id.equals(categoryDto.getId())) {
            logger.warn("ID mismatch: path ID {} doesn't match body ID {}", id, categoryDto.getId());
            throw new RuntimeException("Id from path does not match with id from request");
        }

        Category category = categoryMapper.toEntity(categoryDto);
        Category updatedCategory = categoryService.update(id, category);
        logger.info("Category with id {} updated", id);

        CategoryDTO responseDto = categoryMapper.toDto(updatedCategory);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        try {
            Category category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(categoryMapper.toDto(category));
        } catch (CategoryNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}