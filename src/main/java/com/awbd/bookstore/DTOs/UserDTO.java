package com.awbd.bookstore.DTOs;

import com.awbd.bookstore.models.User.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private Role role;
    private List<Long> orderIds;
    private Long cartId;
    private List<Long> reviewIds;
    private Long wishlistId;

    public UserDTO(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }
    public UserDTO(Long id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }
}