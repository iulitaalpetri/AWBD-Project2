package com.awbd.bookstore.mappers;

import com.awbd.bookstore.DTOs.WishlistDTO;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.models.User;
import com.awbd.bookstore.models.Wishlist;
import com.awbd.bookstore.repositories.BookRepository;
import com.awbd.bookstore.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WishlistMapper {
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Autowired
    public WishlistMapper(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public WishlistDTO toDto(Wishlist wishlist) {
        WishlistDTO dto = new WishlistDTO();
        dto.setId(wishlist.getId());

        if (wishlist.getUser() != null) {
            dto.setUserId(wishlist.getUser().getId());
        }

        if (wishlist.getBooks() != null && !wishlist.getBooks().isEmpty()) {
            List<Long> bookIds = wishlist.getBooks().stream()
                    .map(Book::getId)
                    .collect(Collectors.toList());
            dto.setBookIds(bookIds);
        }

        return dto;
    }

    public Wishlist toEntity(WishlistDTO dto) {
        Wishlist wishlist = new Wishlist();

        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + dto.getUserId()));
            wishlist.setUser(user);
        }

        if (dto.getBookIds() != null && !dto.getBookIds().isEmpty()) {
            List<Book> books = dto.getBookIds().stream()
                    .map(id -> bookRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + id)))
                    .collect(Collectors.toList());
            wishlist.setBooks((HashSet<Book>) books);
        }

        return wishlist;
    }

    public List<WishlistDTO> toDtoList(List<Wishlist> wishlists) {
        return wishlists.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateEntityFromDto(WishlistDTO dto, Wishlist wishlist) {
        if (dto.getUserId() != null) {
            User user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + dto.getUserId()));
            wishlist.setUser(user);
        }

        if (dto.getBookIds() != null) {
            List<Book> currentBooks = new ArrayList<>(wishlist.getBooks());

            currentBooks.forEach(book -> {
                if (!dto.getBookIds().contains(book.getId())) {
                    wishlist.removeBook(book);
                }
            });

            dto.getBookIds().forEach(bookId -> {
                boolean exists = wishlist.getBooks().stream()
                        .anyMatch(b -> b.getId() == bookId);

                if (!exists) {
                    Book book = bookRepository.findById(bookId)
                            .orElseThrow(() -> new EntityNotFoundException("Book not found with id: " + bookId));
                    wishlist.addBook(book);
                }
            });
        }
    }
}