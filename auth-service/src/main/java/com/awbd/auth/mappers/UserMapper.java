package com.awbd.auth.mappers;

import com.awbd.auth.DTOs.UserDTO;
import com.awbd.auth.models.User;
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