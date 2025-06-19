package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.UserDTO;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.models.Order;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.models.Wishlist;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO toDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());

        if (user.getOrders() != null && !user.getOrders().isEmpty()) {
            List<Long> orderIds = user.getOrders().stream()
                    .map(Order::getId)
                    .collect(Collectors.toList());
            dto.setOrderIds(orderIds);
        }

        if (user.getCart() != null) {
            dto.setCartId(user.getCart().getId());
        }

        if (user.getReviews() != null && !user.getReviews().isEmpty()) {
            List<Long> reviewIds = user.getReviews().stream()
                    .map(Review::getId)
                    .collect(Collectors.toList());
            dto.setReviewIds(reviewIds);
        }

        if (user.getWishlist() != null) {
            dto.setWishlistId(user.getWishlist().getId());
        }

        return dto;
    }

    public User toEntity(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());

        if (dto.getPassword() != null) {
            user.setPassword(dto.getPassword());
        }

        user.setRole(dto.getRole());

        return user;
    }

    public List<UserDTO> toDtoList(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(UserDTO dto, User user) {
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }

        if (dto.getPassword() != null) {
            user.setPassword(dto.getPassword());
        }

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

    }
}