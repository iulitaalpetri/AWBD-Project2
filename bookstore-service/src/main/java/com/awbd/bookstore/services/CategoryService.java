package com.awbd.bookstore.services;

import com.awbd.bookstore.exceptions.*;
import com.awbd.bookstore.exceptions.category.CategoryAlreadyExistsException;
import com.awbd.bookstore.exceptions.category.CategoryNotFoundException;
import com.awbd.bookstore.exceptions.category.DuplicateCategoryException;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.repositories.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category createCategory(Category category) {
        logger.info("Creating category with name: '{}'", category.getName());

        if (categoryRepository.existsByName(category.getName())) {
            logger.warn("Category creation failed - category already exists: '{}'", category.getName());
            throw new CategoryAlreadyExistsException("Category with name '" + category.getName() + "' already exists");
        }

        Category savedCategory = categoryRepository.save(category);
        logger.info("Category successfully created with ID: {} - '{}'",
                savedCategory.getId(), savedCategory.getName());

        return savedCategory;
    }

    public Category getCategoryById(Long id) {
        logger.debug("Finding category by ID: {}", id);

        return categoryRepository.findById(id)
                .map(category -> {
                    logger.debug("Category found - ID: {}, Name: '{}'", category.getId(), category.getName());
                    return category;
                })
                .orElseThrow(() -> {
                    logger.warn("Category not found with ID: {}", id);
                    return new CategoryNotFoundException("Category with ID " + id + " not found");
                });
    }

    public List<Book> getBooksInCategory(Long categoryId) {
        logger.debug("Retrieving books for category ID: {}", categoryId);

        Category category = getCategoryById(categoryId);
        List<Book> books = category.getBooks();

        logger.info("Found {} books in category '{}' (ID: {})",
                books.size(), category.getName(), categoryId);

        return books;
    }

    public void delete(Long id) {
        logger.info("Deleting category with ID: {}", id);

        if (!categoryRepository.existsById(id)) {
            logger.warn("Category deletion failed - category not found with ID: {}", id);
            throw new CategoryNotFoundException("Category with ID " + id + " not found");
        }

        // Log category name before deletion for better tracking
        Category categoryToDelete = categoryRepository.findById(id).orElse(null);
        String categoryName = categoryToDelete != null ? categoryToDelete.getName() : "Unknown";

        categoryRepository.deleteById(id);
        logger.info("Category successfully deleted - ID: {}, Name: '{}'", id, categoryName);
    }

    public Category update(Long id, Category updatedCategory) {
        logger.info("Updating category with ID: {} - New name: '{}'", id, updatedCategory.getName());

        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    String oldName = existingCategory.getName();
                    String oldDescription = existingCategory.getDescription();

                    // Verifică dacă noua categorie există deja la alta cat
                    if (!existingCategory.getName().equals(updatedCategory.getName()) &&
                            categoryRepository.existsByName(updatedCategory.getName())) {
                        logger.warn("Category update failed - duplicate name: '{}' for ID: {}",
                                updatedCategory.getName(), id);
                        throw new DuplicateCategoryException("Category with name '" + updatedCategory.getName() + "' already exists");
                    }

                    existingCategory.setName(updatedCategory.getName());
                    existingCategory.setDescription(updatedCategory.getDescription());

                    Category savedCategory = categoryRepository.save(existingCategory);

                    logger.info("Category successfully updated - ID: {}, Name: '{}' -> '{}', Description: '{}' -> '{}'",
                            id, oldName, savedCategory.getName(),
                            oldDescription != null ? oldDescription : "null",
                            savedCategory.getDescription() != null ? savedCategory.getDescription() : "null");

                    return savedCategory;
                })
                .orElseThrow(() -> {
                    logger.warn("Category update failed - category not found with ID: {}", id);
                    return new CategoryNotFoundException("Category with ID " + id + " not found");
                });
    }

    public List<Category> getAllCategories() {
        logger.debug("Retrieving all categories");

        List<Category> categories = categoryRepository.findAll();
        logger.info("Retrieved {} categories", categories.size());

        // Log category names for debugging if needed
        if (logger.isDebugEnabled()) {
            categories.forEach(category ->
                    logger.debug("Category: ID={}, Name='{}'", category.getId(), category.getName()));
        }

        return categories;
    }
}