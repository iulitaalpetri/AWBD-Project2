package com.awbd.bookstore.clients;

import com.awbd.bookstore.DTOs.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/users/{id}/info")
    UserDTO getUserInfo(@PathVariable("id") Long id);

    @PostMapping("/api/users/validate")
    Map<String, Object> validateUser(@RequestParam Long userId);

    @GetMapping("/api/users/by-username/{username}")
    UserDTO getUserByUsername(@PathVariable("username") String username);
}