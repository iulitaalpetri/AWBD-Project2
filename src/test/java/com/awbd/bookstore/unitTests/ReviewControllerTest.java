package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.ReviewDTO;
import com.awbd.bookstore.controllers.ReviewController;
import com.awbd.bookstore.exceptions.token.InvalidTokenException;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.ReviewMapper;
import com.awbd.bookstore.models.Review;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.ReviewService;
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

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private ReviewController reviewController;

    private User testUser;
    private User adminUser;
    private Review testReview;
    private ReviewDTO testReviewDTO;
    private final String validAuthHeader = "Bearer valid-jwt-token";
    private final String invalidAuthHeader = "Invalid header";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(User.Role.USER);

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setRole(User.Role.ADMIN);

        testReview = new Review();
        testReview.setId(1L);
        testReview.setContent("Great book!");
        testReview.setRating(5);
        testReview.setUser(testUser);

        testReviewDTO = new ReviewDTO();
        testReviewDTO.setId(1L);
        testReviewDTO.setContent("Great book!");
        testReviewDTO.setRating(5);
        testReviewDTO.setUserId(1L);
        testReviewDTO.setBookId(1L);
    }

    @Test
    void createReview_Success() {
        Long bookId = 1L;
        Review savedReview = new Review();
        savedReview.setId(1L);
        savedReview.setContent("Great book!");
        savedReview.setRating(5);

        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(reviewMapper.toEntity(testReviewDTO)).thenReturn(testReview);
        when(reviewService.addReview(testReview, bookId, testUser.getId())).thenReturn(savedReview);
        when(reviewMapper.toDto(savedReview)).thenReturn(testReviewDTO);

        ResponseEntity<ReviewDTO> result = reviewController.createReview(bookId, testReviewDTO, validAuthHeader);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(testReviewDTO, result.getBody());
        assertEquals(URI.create("/api/reviews/1"), result.getHeaders().getLocation());
        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(userService, times(1)).findByUsername("testuser");
        verify(reviewMapper, times(1)).toEntity(testReviewDTO);
        verify(reviewService, times(1)).addReview(testReview, bookId, testUser.getId());
        verify(reviewMapper, times(1)).toDto(savedReview);
    }

    @Test
    void createReview_InvalidAuthHeader_ThrowsInvalidTokenException() {
        Long bookId = 1L;

        assertThrows(InvalidTokenException.class, () -> {
            reviewController.createReview(bookId, testReviewDTO, invalidAuthHeader);
        });

        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(reviewService, never()).addReview(any(), any(), any());
    }

    @Test
    void createReview_NullAuthHeader_ThrowsInvalidTokenException() {
        Long bookId = 1L;

        assertThrows(InvalidTokenException.class, () -> {
            reviewController.createReview(bookId, testReviewDTO, null);
        });

        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(reviewService, never()).addReview(any(), any(), any());
    }

    @Test
    void createReview_InvalidToken_ThrowsInvalidTokenException() {
        Long bookId = 1L;
        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn(null);

        assertThrows(InvalidTokenException.class, () -> {
            reviewController.createReview(bookId, testReviewDTO, validAuthHeader);
        });

        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(reviewService, never()).addReview(any(), any(), any());
    }

    @Test
    void createReview_UserNotFound_ThrowsUserNotFoundException() {
        Long bookId = 1L;
        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            reviewController.createReview(bookId, testReviewDTO, validAuthHeader);
        });

        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(userService, times(1)).findByUsername("testuser");
        verify(reviewService, never()).addReview(any(), any(), any());
    }

    @Test
    void getReviewsByBookId_Success() {
        Long bookId = 1L;
        List<Review> reviews = Arrays.asList(testReview);
        List<ReviewDTO> reviewDTOs = Arrays.asList(testReviewDTO);

        when(reviewService.getReviewsByBookId(bookId)).thenReturn(reviews);
        when(reviewMapper.toDtoList(reviews)).thenReturn(reviewDTOs);

        List<ReviewDTO> result = reviewController.getReviewsByBookId(bookId);

        assertEquals(reviewDTOs, result);
        assertEquals(1, result.size());
        assertEquals("Great book!", result.get(0).getContent());
        verify(reviewService, times(1)).getReviewsByBookId(bookId);
        verify(reviewMapper, times(1)).toDtoList(reviews);
    }

    @Test
    void getReviewsByBookId_EmptyList() {
        Long bookId = 1L;
        List<Review> emptyReviews = Arrays.asList();
        List<ReviewDTO> emptyReviewDTOs = Arrays.asList();

        when(reviewService.getReviewsByBookId(bookId)).thenReturn(emptyReviews);
        when(reviewMapper.toDtoList(emptyReviews)).thenReturn(emptyReviewDTOs);

        List<ReviewDTO> result = reviewController.getReviewsByBookId(bookId);

        assertEquals(emptyReviewDTOs, result);
        assertEquals(0, result.size());
        verify(reviewService, times(1)).getReviewsByBookId(bookId);
        verify(reviewMapper, times(1)).toDtoList(emptyReviews);
    }

    @Test
    void deleteReview_OwnerSuccess() {
        Long reviewId = 1L;
        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(reviewService.getReviewById(reviewId)).thenReturn(testReview);

        ResponseEntity<Void> result = reviewController.deleteReview(reviewId, validAuthHeader);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertNull(result.getBody());
        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(userService, times(1)).findByUsername("testuser");
        verify(reviewService, times(1)).getReviewById(reviewId);
        verify(reviewService, times(1)).deleteReview(reviewId);
    }

    @Test
    void deleteReview_AdminSuccess() {
        Long reviewId = 1L;
        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("admin");
        when(userService.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(reviewService.getReviewById(reviewId)).thenReturn(testReview);

        ResponseEntity<Void> result = reviewController.deleteReview(reviewId, validAuthHeader);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertNull(result.getBody());
        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(userService, times(1)).findByUsername("admin");
        verify(reviewService, times(1)).getReviewById(reviewId);
        verify(reviewService, times(1)).deleteReview(reviewId);
    }

    @Test
    void deleteReview_UnauthorizedUser_ThrowsRuntimeException() {
        Long reviewId = 1L;
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setUsername("otheruser");
        otherUser.setRole(User.Role.USER);

        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("otheruser");
        when(userService.findByUsername("otheruser")).thenReturn(Optional.of(otherUser));
        when(reviewService.getReviewById(reviewId)).thenReturn(testReview);

        assertThrows(RuntimeException.class, () -> {
            reviewController.deleteReview(reviewId, validAuthHeader);
        });

        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(userService, times(1)).findByUsername("otheruser");
        verify(reviewService, times(1)).getReviewById(reviewId);
        verify(reviewService, never()).deleteReview(reviewId);
    }

    @Test
    void deleteReview_InvalidAuthHeader_ThrowsInvalidTokenException() {
        Long reviewId = 1L;

        assertThrows(InvalidTokenException.class, () -> {
            reviewController.deleteReview(reviewId, invalidAuthHeader);
        });

        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(reviewService, never()).deleteReview(reviewId);
    }

    @Test
    void deleteReview_NullAuthHeader_ThrowsInvalidTokenException() {
        Long reviewId = 1L;

        assertThrows(InvalidTokenException.class, () -> {
            reviewController.deleteReview(reviewId, null);
        });

        verify(jwtUtil, never()).getUsernameFromToken(anyString());
        verify(reviewService, never()).deleteReview(reviewId);
    }

    @Test
    void deleteReview_InvalidToken_ThrowsInvalidTokenException() {
        Long reviewId = 1L;
        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn(null);

        assertThrows(InvalidTokenException.class, () -> {
            reviewController.deleteReview(reviewId, validAuthHeader);
        });

        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(reviewService, never()).deleteReview(reviewId);
    }

    @Test
    void deleteReview_UserNotFound_ThrowsUserNotFoundException() {
        Long reviewId = 1L;
        when(jwtUtil.getUsernameFromToken("valid-jwt-token")).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> {
            reviewController.deleteReview(reviewId, validAuthHeader);
        });

        verify(jwtUtil, times(1)).getUsernameFromToken("valid-jwt-token");
        verify(userService, times(1)).findByUsername("testuser");
        verify(reviewService, never()).deleteReview(reviewId);
    }
}
