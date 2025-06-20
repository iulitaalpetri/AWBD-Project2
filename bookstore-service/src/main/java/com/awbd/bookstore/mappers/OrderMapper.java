package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.OrderDTO;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.Order;
import com.awbd.bookstore.repositories.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private final BookRepository bookRepository;

    // CONSTRUCTOR MODIFICAT - eliminat UserRepository
    @Autowired
    public OrderMapper(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public OrderDTO toDto(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());

        // MODIFICAT: folosim direct userId
        dto.setUserId(order.getUserId());

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

        // MODIFICAT: setăm userId direct
        order.setUserId(orderDTO.getUserId());

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