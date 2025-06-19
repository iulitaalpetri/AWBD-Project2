package com.awbd.bookstore.DTOs;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Long id;
    private Long userId;
    private Set<Long> bookIds;


    public CartDTO(Long userId, Set<Long> bookIds) {
        this.userId = userId;
        this.bookIds = bookIds;
    }


    public CartDTO(Long userId) {
        this.userId = userId;
    }
}
