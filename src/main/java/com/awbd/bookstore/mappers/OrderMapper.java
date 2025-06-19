package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.OrderDTO;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Order;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Autowired
    public OrderMapper(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public OrderDTO toDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setBookIds(order.getBooks().stream()
                .map(Book::getId)
                .collect(Collectors.toSet()));
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalPrice(order.getTotalPrice());
        if (order.getSale() != null) {
            dto.setSaleId(order.getSale().getId());
        }
        return dto;
    }

    public Order toEntity(OrderDTO orderDTO) {
        Order order = new Order();


        User user = userRepository.findById(orderDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + orderDTO.getUserId()));
        order.setUser(user);


        Set<Book> books = orderDTO.getBookIds().stream()
                .map(bookId -> bookRepository.findById(bookId)
                        .orElseThrow(() -> new RuntimeException("Book not found with id: " + bookId)))
                .collect(Collectors.toSet());
        order.setBooks(books);

        order.setOrderDate(orderDTO.getOrderDate());
        order.setTotalPrice(orderDTO.getTotalPrice());

        return order;
    }

    public List<OrderDTO> toDtoList(List<Order> orders) {
        return orders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}