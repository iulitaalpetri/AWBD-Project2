package com.awbd.bookstore.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private Set<Long> bookIds;
    private LocalDateTime orderDate;
    private Double totalPrice;
    private Long saleId;


    public OrderDTO(Long userId, Set<Long> bookIds, Double totalPrice) {
        this.userId = userId;
        this.bookIds = bookIds;
        this.orderDate = LocalDateTime.now();
        this.totalPrice = totalPrice;
    }

    public OrderDTO(Long userId, Set<Long> bookIds, Double totalPrice, Long saleId) {
        this.userId = userId;
        this.bookIds = bookIds;
        this.orderDate = LocalDateTime.now();
        this.totalPrice = totalPrice;
        this.saleId = saleId;
    }
}