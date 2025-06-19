package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.CartDTO;
import com.awbd.bookstore.models.Cart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CartMapper {
    private final BookMapper bookMapper;

    @Autowired
    public CartMapper(BookMapper bookMapper) {
        this.bookMapper = bookMapper;
    }

    public CartDTO toDto(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());

        if (cart.getBooks() != null && !cart.getBooks().isEmpty()) {
            Set<Long> bookIds = cart.getBooks().stream()
                    .map(book -> book.getId())
                    .collect(Collectors.toSet());
            dto.setBookIds(bookIds);
        }

        return dto;
    }
}