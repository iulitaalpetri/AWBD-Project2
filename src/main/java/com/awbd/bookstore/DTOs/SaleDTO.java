package com.awbd.bookstore.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaleDTO {
    private Long id;
    private String saleCode;
    private Double discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Boolean isActive;
    private List<Long> categoryIds;

    public SaleDTO(Double discountPercentage, LocalDate startDate, LocalDate endDate,
                   String description, Boolean isActive, List<Long> categoryIds) {
        this.discountPercentage = discountPercentage;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.isActive = isActive;
        this.categoryIds = categoryIds;
    }

    public SaleDTO(Double discountPercentage, LocalDate startDate, LocalDate endDate) {
        this.discountPercentage = discountPercentage;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
    }
}
