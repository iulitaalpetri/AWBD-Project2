package com.awbd.bookstore.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {
    private Long id;
    private Long userId;
    private List<Long> bookIds;

    public WishlistDTO(Long userId, List<Long> bookIds) {
        this.userId = userId;
        this.bookIds = bookIds;
    }

    public WishlistDTO(Long userId) {
        this.userId = userId;
    }
}