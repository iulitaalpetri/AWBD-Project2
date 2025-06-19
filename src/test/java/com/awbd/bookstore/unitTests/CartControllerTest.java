package com.awbd.bookstore.unitTests;


import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.DTOs.CartDTO;
import com.awbd.bookstore.controllers.CartController;
import com.awbd.bookstore.exceptions.token.InvalidTokenException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.CartMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.CartService;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private CartController cartController;

    private User user;
    private Cart cart;
    private CartDTO cartDTO;
    private Book book;
    private BookDTO bookDTO;
    private String validAuthHeader;
    private String token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);

        cartDTO = new CartDTO();
        cartDTO.setId(1L);
        cartDTO.setUserId(1L);
        cartDTO.setBookIds(Set.of(1L));

        book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        bookDTO = new BookDTO();
        bookDTO.setId(1L);
        bookDTO.setTitle("Test Book");

        token = "validToken123";
        validAuthHeader = "Bearer " + token;
    }

    @Test
    void addBookToCart_Success() {
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartService.getCartByUserId(1L)).thenReturn(Optional.ofNullable(cart));
        doNothing().when(cartService).addBookToCart(1L, 1L);
        when(cartMapper.toDto(cart)).thenReturn(cartDTO);

        ResponseEntity<CartDTO> result = cartController.addBookToCart(1L, validAuthHeader);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(cartDTO, result.getBody());
        verify(jwtUtil, times(1)).getUsernameFromToken(token);
        verify(userService, times(1)).findByUsername("testuser");
        verify(cartService, times(1)).getCartByUserId(1L);
        verify(cartService, times(1)).addBookToCart(1L, 1L);
        verify(cartMapper, times(1)).toDto(cart);
    }

    @Test
    void getCartBooks_Success() {
        List<Book> booksInCart = Arrays.asList(book);
        List<BookDTO> bookDTOs = Arrays.asList(bookDTO);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartService.getCartByUserId(1L)).thenReturn(Optional.ofNullable(cart));
        when(cartService.getBooksInCart(1L)).thenReturn(booksInCart);
        when(bookMapper.toDtoList(booksInCart)).thenReturn(bookDTOs);

        ResponseEntity<List<BookDTO>> result = cartController.getCartBooks(validAuthHeader);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Test Book", result.getBody().get(0).getTitle());
        verify(jwtUtil, times(1)).getUsernameFromToken(token);
        verify(userService, times(1)).findByUsername("testuser");
        verify(cartService, times(1)).getCartByUserId(1L);
        verify(cartService, times(1)).getBooksInCart(1L);
        verify(bookMapper, times(1)).toDtoList(booksInCart);
    }

    @Test
    void getTotalPrice_Success() {
        double totalPrice = 29.99;

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartService.getCartByUserId(1L)).thenReturn(Optional.ofNullable(cart));
        when(cartService.calculateTotalPrice(1L)).thenReturn(totalPrice);

        ResponseEntity<Double> result = cartController.getTotalPrice(validAuthHeader);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(totalPrice, result.getBody());
        verify(jwtUtil, times(1)).getUsernameFromToken(token);
        verify(userService, times(1)).findByUsername("testuser");
        verify(cartService, times(1)).getCartByUserId(1L);
        verify(cartService, times(1)).calculateTotalPrice(1L);
    }
}