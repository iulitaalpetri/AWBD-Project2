package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.SaleDTO;
import com.awbd.bookstore.models.Category;
import com.awbd.bookstore.models.Sale;
import com.awbd.bookstore.repositories.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SaleMapper {
    private final CategoryRepository categoryRepository;

    @Autowired
    public SaleMapper(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public SaleDTO toDto(Sale sale) {
        SaleDTO dto = new SaleDTO();
        dto.setId(sale.getId());
        dto.setSaleCode(sale.getSaleCode());
        dto.setDiscountPercentage(sale.getDiscountPercentage());
        dto.setStartDate(sale.getStartDate());
        dto.setEndDate(sale.getEndDate());
        dto.setDescription(sale.getDescription());
        dto.setIsActive(sale.getIsActive());

        if (sale.getCategories() != null && !sale.getCategories().isEmpty()) {
            List<Long> categoryIds = sale.getCategories().stream()
                    .map(Category::getId)
                    .collect(Collectors.toList());
            dto.setCategoryIds(categoryIds);
        }

        return dto;
    }

    public Sale toEntity(SaleDTO dto) {
        Sale sale = new Sale();
        sale.setDiscountPercentage(dto.getDiscountPercentage());
        sale.setStartDate(dto.getStartDate());
        sale.setEndDate(dto.getEndDate());
        sale.setDescription(dto.getDescription());

        if (dto.getIsActive() != null) {
            sale.setIsActive(dto.getIsActive());
        } else {
            sale.setIsActive(true);
        }

        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            dto.getCategoryIds().forEach(categoryId -> {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
                sale.addCategory(category);
            });
        }

        return sale;
    }

    public List<SaleDTO> toDtoList(List<Sale> sales) {
        return sales.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(SaleDTO dto, Sale sale) {
        sale.setDiscountPercentage(dto.getDiscountPercentage());
        sale.setStartDate(dto.getStartDate());
        sale.setEndDate(dto.getEndDate());
        sale.setDescription(dto.getDescription());

        if (dto.getIsActive() != null) {
            sale.setIsActive(dto.getIsActive());
        }

        if (dto.getCategoryIds() != null) {
            List<Category> categoriesToRemove = new ArrayList<>(sale.getCategories());
            categoriesToRemove.forEach(category -> {
                if (!dto.getCategoryIds().contains(category.getId())) {
                    sale.removeCategory(category);
                }
            });

            dto.getCategoryIds().forEach(categoryId -> {
                boolean exists = sale.getCategories().stream()
                        .anyMatch(c -> c.getId() == categoryId);

                if (!exists) {
                    Category category = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));
                    sale.addCategory(category);
                }
            });
        }
    }
}