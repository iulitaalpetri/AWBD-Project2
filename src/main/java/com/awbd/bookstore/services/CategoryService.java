package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.*;
import com.awbd.bookstore.exceptions.category.CategoryAlreadyExistsException;
import com.awbd.bookstore.exceptions.category.CategoryNotFoundException;
import com.awbd.bookstore.exceptions.category.DuplicateCategoryException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.repositories.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new CategoryAlreadyExistsException("Category with name '" + category.getName() + "' already exists");
        }
        return categoryRepository.save(category);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category with ID " + id + " not found"));
    }

    public List<Book> getBooksInCategory(Long categoryId) {
        Category category = getCategoryById(categoryId);
        return category.getBooks();
    }

    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException("Category with ID " + id + " not found");
        }
        categoryRepository.deleteById(id);
    }

    public Category update(Long id, Category updatedCategory) {
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    // Verifică dacă noua categorie există deja la alta cat
                    if (!existingCategory.getName().equals(updatedCategory.getName()) &&
                            categoryRepository.existsByName(updatedCategory.getName())) {
                        throw new DuplicateCategoryException("Category with name '" + updatedCategory.getName() + "' already exists");
                    }

                    existingCategory.setName(updatedCategory.getName());
                    existingCategory.setDescription(updatedCategory.getDescription());

                    return categoryRepository.save(existingCategory);
                })
                .orElseThrow(() -> new CategoryNotFoundException("Category with ID " + id + " not found"));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}