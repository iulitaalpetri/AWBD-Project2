package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.LoginRequestDTO;
import com.awbd.bookstore.DTOs.RegisterRequestDTO;
import com.awbd.bookstore.DTOs.UserDTO;
import com.awbd.bookstore.controllers.UserController;
import com.awbd.bookstore.exceptions.user.InvalidRoleException;
import com.awbd.bookstore.mappers.UserMapper;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserController userController;

    @Test
    void registerUser_Success() {
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Test123!");
        registerRequest.setRole("USER");

        User createdUser = new User();
        createdUser.setId(1L);
        createdUser.setUsername("testuser");
        createdUser.setPassword("Test123!");
        createdUser.setRole(User.Role.USER);

        UserDTO expectedUserDTO = new UserDTO();
        expectedUserDTO.setId(1L);
        expectedUserDTO.setUsername("testuser");
        expectedUserDTO.setRole(User.Role.USER);

        when(userService.create(any(User.class))).thenReturn(createdUser);
        when(userMapper.toDto(any(User.class))).thenReturn(expectedUserDTO);

        ResponseEntity<UserDTO> response = userController.registerUser(registerRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals(1L, response.getBody().getId());
        assertEquals(User.Role.USER, response.getBody().getRole());

        assertEquals("/api/users/1", response.getHeaders().getLocation().toString());

        verify(userService, times(1)).create(any(User.class));
        verify(userMapper, times(1)).toDto(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test123!");

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("testuser");
        existingUser.setPassword(Base64.getEncoder().encodeToString("Test123!".getBytes()));
        existingUser.setRole(User.Role.USER);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setRole(User.Role.USER);

        String expectedToken = "jwt.token.here";

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(existingUser));
        when(userMapper.toDto(existingUser)).thenReturn(userDTO);
        when(jwtUtil.generateToken("testuser", User.Role.USER)).thenReturn(expectedToken);

        ResponseEntity<Map<String, Object>> response = userController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        Map<String, Object> responseBody = response.getBody();
        assertTrue(responseBody.containsKey("user"));
        assertTrue(responseBody.containsKey("token"));

        UserDTO returnedUser = (UserDTO) responseBody.get("user");
        assertEquals("testuser", returnedUser.getUsername());
        assertEquals(1L, returnedUser.getId());
        assertEquals(User.Role.USER, returnedUser.getRole());

        String returnedToken = (String) responseBody.get("token");
        assertEquals(expectedToken, returnedToken);

        verify(userService, times(1)).findByUsername("testuser");
        verify(userMapper, times(1)).toDto(existingUser);
        verify(jwtUtil, times(1)).generateToken("testuser", User.Role.USER);
    }

    @Test
    void getUser_Success_SameUser() {
        Long userId = 1L;
        String authHeader = "Bearer jwt.token.here";
        String username = "testuser";

        User authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setUsername("testuser");
        authenticatedUser.setRole(User.Role.USER);

        User foundUser = new User();
        foundUser.setId(1L);
        foundUser.setUsername("testuser");
        foundUser.setRole(User.Role.USER);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("testuser");
        userDTO.setRole(User.Role.USER);

        when(jwtUtil.getUsernameFromToken("jwt.token.here")).thenReturn(username);
        when(jwtUtil.getRoleFromToken("jwt.token.here")).thenReturn(User.Role.USER);
        when(userService.findByUsername(username)).thenReturn(Optional.of(authenticatedUser));
        when(userService.findById(userId)).thenReturn(Optional.of(foundUser));
        when(userMapper.toDto(foundUser)).thenReturn(userDTO);

        ResponseEntity<UserDTO> response = userController.getUser(userId, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getUsername());
        assertEquals(1L, response.getBody().getId());
        assertEquals(User.Role.USER, response.getBody().getRole());

        verify(jwtUtil, times(1)).getUsernameFromToken("jwt.token.here");
        verify(jwtUtil, times(1)).getRoleFromToken("jwt.token.here");
        verify(userService, times(1)).findByUsername(username);
        verify(userService, times(1)).findById(userId);
        verify(userMapper, times(1)).toDto(foundUser);
    }

    @Test
    void getUser_Success_AdminUser() {
        Long userId = 2L;
        String authHeader = "Bearer admin.jwt.token.here";
        String adminUsername = "admin";

        User targetUser = new User();
        targetUser.setId(2L);
        targetUser.setUsername("otheruser");
        targetUser.setRole(User.Role.USER);

        UserDTO userDTO = new UserDTO();
        userDTO.setId(2L);
        userDTO.setUsername("otheruser");
        userDTO.setRole(User.Role.USER);

        when(jwtUtil.getUsernameFromToken("admin.jwt.token.here")).thenReturn(adminUsername);
        when(jwtUtil.getRoleFromToken("admin.jwt.token.here")).thenReturn(User.Role.ADMIN);
        when(userService.findById(userId)).thenReturn(Optional.of(targetUser));
        when(userMapper.toDto(targetUser)).thenReturn(userDTO);

        ResponseEntity<UserDTO> response = userController.getUser(userId, authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("otheruser", response.getBody().getUsername());
        assertEquals(2L, response.getBody().getId());
        assertEquals(User.Role.USER, response.getBody().getRole());

        verify(jwtUtil, times(1)).getUsernameFromToken("admin.jwt.token.here");
        verify(jwtUtil, times(1)).getRoleFromToken("admin.jwt.token.here");
        verify(userService, never()).findByUsername(any());
        verify(userService, times(1)).findById(userId);
        verify(userMapper, times(1)).toDto(targetUser);
    }

    @Test
    void updateUser_Success() {

        Long userId = 1L;

        UserDTO inputUserDTO = new UserDTO();
        inputUserDTO.setId(1L);
        inputUserDTO.setUsername("updateduser");
        inputUserDTO.setRole(User.Role.USER);

        User inputUser = new User();
        inputUser.setId(1L);
        inputUser.setUsername("updateduser");
        inputUser.setRole(User.Role.USER);

        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("updateduser");
        updatedUser.setRole(User.Role.USER);

        UserDTO responseUserDTO = new UserDTO();
        responseUserDTO.setId(1L);
        responseUserDTO.setUsername("updateduser");
        responseUserDTO.setRole(User.Role.USER);

        when(userMapper.toEntity(inputUserDTO)).thenReturn(inputUser);
        when(userService.update(userId, inputUser)).thenReturn(updatedUser);
        when(userMapper.toDto(updatedUser)).thenReturn(responseUserDTO);

        ResponseEntity<UserDTO> response = userController.updateUser(userId, inputUserDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("updateduser", response.getBody().getUsername());
        assertEquals(1L, response.getBody().getId());
        assertEquals(User.Role.USER, response.getBody().getRole());

        verify(userMapper, times(1)).toEntity(inputUserDTO);
        verify(userService, times(1)).update(userId, inputUser);
        verify(userMapper, times(1)).toDto(updatedUser);
    }

    @Test
    void deleteUser_Success() {

        Long userId = 1L;


        doNothing().when(userService).delete(userId);

        ResponseEntity<Void> response = userController.deleteUser(userId);


        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());


        verify(userService, times(1)).delete(userId);
    }





}