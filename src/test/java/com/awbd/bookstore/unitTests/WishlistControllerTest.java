package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.controllers.WishlistController;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.mappers.WishlistMapper;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.services.UserService;
import com.awbd.bookstore.services.WishlistService;
import com.awbd.bookstore.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistControllerTest {

    @Mock
    private WishlistService wishlistService;

    @Mock
    private WishlistMapper wishlistMapper;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private WishlistController wishlistController;

    private User testUser;
    private Wishlist testWishlist;
    private Book testBook;
    private BookDTO testBookDTO;
    private String validToken;
    private String validAuthHeader;
    private String username;

    @BeforeEach
    void setUp() {
        username = "testuser";
        validToken = "valid.jwt.token";
        validAuthHeader = "Bearer " + validToken;

        testWishlist = new Wishlist();
        testWishlist.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(username);
        testUser.setWishlist(testWishlist);

        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("Test Book");
        Author author = new Author();
        author.setName("Test Author");
        testBook.setAuthor(author);

        testBookDTO = new BookDTO();
        testBookDTO.setId(1L);
        testBookDTO.setTitle("Test Book");
    }

    @Test
    void addBookToWishlist_Success() {
        Long bookId = 1L;
        Long wishlistId = 1L;

        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(wishlistService.addBookToWishlist(wishlistId, bookId)).thenReturn(testBook);
        when(bookMapper.toDto(testBook)).thenReturn(testBookDTO);

        ResponseEntity<BookDTO> result = wishlistController.addBookToWishlist(validAuthHeader, bookId);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(testBookDTO, result.getBody());
        assertEquals(URI.create("/api/wishlists/1/1"), result.getHeaders().getLocation());

        verify(jwtUtil, times(1)).getUsernameFromToken(validToken);
        verify(userService, times(1)).findByUsername(username);
        verify(wishlistService, times(1)).addBookToWishlist(wishlistId, bookId);
        verify(bookMapper, times(1)).toDto(testBook);
    }

    @Test
    void removeBookFromWishlist_Success() {
        Long bookId = 1L;
        Long wishlistId = 1L;

        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        doNothing().when(wishlistService).removeBookFromWishlist(wishlistId, bookId);

        ResponseEntity<Void> result = wishlistController.removeBookFromWishlist(validAuthHeader, bookId);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertNull(result.getBody());

        verify(jwtUtil, times(1)).getUsernameFromToken(validToken);
        verify(userService, times(1)).findByUsername(username);
        verify(wishlistService, times(1)).removeBookFromWishlist(wishlistId, bookId);
    }

    @Test
    void getUserWishlist_Success() {
        List<Book> books = Arrays.asList(testBook);
        List<BookDTO> bookDTOs = Arrays.asList(testBookDTO);

        when(jwtUtil.getUsernameFromToken(validToken)).thenReturn(username);
        when(userService.findByUsername(username)).thenReturn(Optional.of(testUser));
        when(wishlistService.getBooksByWishlistId(testWishlist.getId())).thenReturn(books);
        when(bookMapper.toDtoList(books)).thenReturn(bookDTOs);

        List<BookDTO> result = wishlistController.getUserWishlist(validAuthHeader);

        assertEquals(bookDTOs, result);
        assertEquals(1, result.size());
        assertEquals(testBookDTO.getTitle(), result.get(0).getTitle());

        verify(jwtUtil, times(1)).getUsernameFromToken(validToken);
        verify(userService, times(1)).findByUsername(username);
        verify(wishlistService, times(1)).getBooksByWishlistId(testWishlist.getId());
        verify(bookMapper, times(1)).toDtoList(books);
    }
}