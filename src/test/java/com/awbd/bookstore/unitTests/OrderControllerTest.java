package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.OrderDTO;
import com.awbd.bookstore.DTOs.OrderRequestDTO;
import com.awbd.bookstore.controllers.OrderController;
import com.awbd.bookstore.exceptions.user.UserNotFoundException;
import com.awbd.bookstore.mappers.OrderMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Cart;
import com.awbd.bookstore.models.Order;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.services.CartService;
import com.awbd.bookstore.services.OrderService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private CartService cartService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderController orderController;

    private User user;
    private Cart cart;
    private Order order;
    private OrderDTO orderDTO;
    private OrderRequestDTO orderRequestDTO;
    private Book book;
    private String validAuthHeader;
    private String token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        cart = new Cart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setBooks(Set.of(book));

        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setOrderDate(LocalDateTime.now());
        order.setTotalPrice(29.99);

        orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setUserId(1L);
        orderDTO.setBookIds(Set.of(1L));
        orderDTO.setOrderDate(LocalDateTime.now());
        orderDTO.setTotalPrice(29.99);

        orderRequestDTO = new OrderRequestDTO();
        orderRequestDTO.setSaleId(1L);

        token = "validToken123";
        validAuthHeader = "Bearer " + token;
    }

    @Test
    void createOrder_Success() {
        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(cartService.getCartByUserId(1L)).thenReturn(Optional.ofNullable(cart));
        when(orderService.createOrder(1L, List.of(1L), 1L)).thenReturn(order);
        when(orderMapper.toDto(order)).thenReturn(orderDTO);
        doNothing().when(cartService).clearCart(1L);

        ResponseEntity<OrderDTO> result = orderController.createOrder(validAuthHeader, orderRequestDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(orderDTO, result.getBody());
        verify(jwtUtil, times(1)).getUsernameFromToken(token);
        verify(userService, times(1)).findByUsername("testuser");
        verify(cartService, times(1)).getCartByUserId(1L);
        verify(orderService, times(1)).createOrder(1L, List.of(1L), 1L);
        verify(cartService, times(1)).clearCart(1L);
        verify(orderMapper, times(1)).toDto(order);
    }

    @Test
    void getUserOrderHistory_Success() {
        List<Order> orders = Arrays.asList(order);
        List<OrderDTO> orderDTOs = Arrays.asList(orderDTO);

        when(jwtUtil.getUsernameFromToken(token)).thenReturn("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(orderService.getUserOrderHistory(user)).thenReturn(orders);
        when(orderMapper.toDtoList(orders)).thenReturn(orderDTOs);

        ResponseEntity<List<OrderDTO>> result = orderController.getUserOrderHistory(validAuthHeader);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals(orderDTO, result.getBody().get(0));
        verify(jwtUtil, times(1)).getUsernameFromToken(token);
        verify(userService, times(1)).findByUsername("testuser");
        verify(orderService, times(1)).getUserOrderHistory(user);
        verify(orderMapper, times(1)).toDtoList(orders);
    }

    @Test
    void getAllOrders_Success() {
        List<Order> orders = Arrays.asList(order);
        List<OrderDTO> orderDTOs = Arrays.asList(orderDTO);

        when(orderService.getAllOrders()).thenReturn(orders);
        when(orderMapper.toDtoList(orders)).thenReturn(orderDTOs);

        ResponseEntity<List<OrderDTO>> result = orderController.getAllOrders();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals(orderDTO, result.getBody().get(0));
        verify(orderService, times(1)).getAllOrders();
        verify(orderMapper, times(1)).toDtoList(orders);
    }

    @Test
    void updateOrder_Success() {
        Order updatedOrder = new Order();
        updatedOrder.setId(1L);
        updatedOrder.setUserId(1L);
        updatedOrder.setTotalPrice(35.99);

        OrderDTO updatedOrderDTO = new OrderDTO();
        updatedOrderDTO.setId(1L);
        updatedOrderDTO.setUserId(1L);
        updatedOrderDTO.setTotalPrice(35.99);

        when(orderService.updateOrder(1L, 1L, orderDTO.getBookIds())).thenReturn(updatedOrder);
        when(orderMapper.toDto(updatedOrder)).thenReturn(updatedOrderDTO);

        ResponseEntity<OrderDTO> result = orderController.updateOrder(1L, orderDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(updatedOrderDTO, result.getBody());
        verify(orderService, times(1)).updateOrder(1L, 1L, orderDTO.getBookIds());
        verify(orderMapper, times(1)).toDto(updatedOrder);
    }
}