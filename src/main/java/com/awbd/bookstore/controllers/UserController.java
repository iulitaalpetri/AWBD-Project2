package com.awbd.bookstore.controllers;

import com.awbd.bookstore.DTOs.LoginRequestDTO;
import com.awbd.bookstore.DTOs.LoginResponseDTO;
import com.awbd.bookstore.DTOs.RegisterRequestDTO;
import com.awbd.bookstore.DTOs.UserDTO;
import com.awbd.bookstore.annotations.RequireAdmin;
import com.awbd.bookstore.exceptions.user.InvalidRoleException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.UserMapper;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.utils.JwtUtil;
import com.awbd.bookstore.utils.TokenBlacklistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;



@RestController
@RequestMapping("/api/users")
public class UserController {
    private UserService userService;
    private UserMapper userMapper;
    private JwtUtil jwtUtil;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final TokenBlacklistService tokenBlacklistService = new TokenBlacklistService();

    public UserController(UserService userService, UserMapper userMapper, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")

    public ResponseEntity<UserDTO> registerUser(
            @RequestBody
            @Valid
            RegisterRequestDTO registerRequest) {

        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(registerRequest.getPassword());

        try {
            User.Role role = User.Role.valueOf(registerRequest.getRole());
            newUser.setRole(role);
        } catch (IllegalArgumentException e) {
            logger.error("Error at role conversion: {}", e.getMessage());
            throw new InvalidRoleException("Invalid role: " + registerRequest.getRole());
        }

        User createdUser = userService.create(newUser);
        logger.info("User created with id {}", createdUser.getId());

        return ResponseEntity.created(URI.create("/api/users/" + createdUser.getId()))
                .body(userMapper.toDto(createdUser));

    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDTO loginRequest) {
        Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String encodedPassword = Base64.getEncoder()
                    .encodeToString(loginRequest.getPassword().getBytes());

            if (encodedPassword.equals(user.getPassword())) {
                logger.info("Authorized: user {}", user.getUsername());
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                logger.info("Token generated for user {}", user.getUsername());

                Map<String, Object> response = new HashMap<>();
                response.put("user", userMapper.toDto(user));
                response.put("token", token);
                response.put("username", user.getUsername());
                response.put("role", user.getRole().name());

                return ResponseEntity.ok(response);
            }
        }

        logger.info("Unauthorized login attempt for username: {}", loginRequest.getUsername());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        System.out.println("Authorization header: " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        User.Role role = jwtUtil.getRoleFromToken(token);

        if (username == null) {
            logger.error("Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // un user poate vedea doar datele sale
        if (role != User.Role.ADMIN) {
            User authenticatedUser = userService.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));


            if (authenticatedUser.getId() != id) {
                logger.warn("User {} attempted to access details of user with ID {}", username, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        // adminul poate vedea datele oricui
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + id + " not found."));

        logger.info("Retrieved user with ID: {}", id);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable
            Long id,

            @RequestBody
            @Valid
            UserDTO userDto) {

        if (userDto.getId() != null && !id.equals(userDto.getId())) {
            logger.warn("ID mismatch: path ID {} doesn't match body ID {}", id, userDto.getId());
            throw new RuntimeException("Id from path does not match with id from request");
        }

        User user = userMapper.toEntity(userDto);
        User updatedUser = userService.update(id, user);
        logger.info("Updated user with ID {}", id);

        return ResponseEntity.ok(userMapper.toDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        logger.info("User with ID {} was deleted", id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);

            tokenBlacklistService.invalidateToken(token);
            logger.info("Token invalidat cu succes");
        }

        logger.info("Logout realizat cu succes");
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> allUsers = userService.getAllUsers();
        List<UserDTO> userDTOs = userMapper.toDtoList(allUsers);

        logger.info("Retrieved {} users for admin", userDTOs.size());
        return ResponseEntity.ok(userDTOs);
    }


}



