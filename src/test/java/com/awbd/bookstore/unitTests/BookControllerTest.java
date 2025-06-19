package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.BookDTO;
import com.awbd.bookstore.mappers.BookMapper;
import com.awbd.bookstore.models.Book;
import com.awbd.bookstore.controllers.BookController;
import com.awbd.bookstore.services.BookService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService bookService;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookController bookController;

    @Test
    void getAllBooks_Success() {
        Book book1 = new Book();
        book1.setId(1L);
        book1.setTitle("Book 1");

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitle("Book 2");

        List<Book> books = Arrays.asList(book1, book2);

        BookDTO bookDTO1 = new BookDTO();
        bookDTO1.setId(1L);
        bookDTO1.setTitle("Book 1");

        BookDTO bookDTO2 = new BookDTO();
        bookDTO2.setId(2L);
        bookDTO2.setTitle("Book 2");

        List<BookDTO> bookDTOs = Arrays.asList(bookDTO1, bookDTO2);

        when(bookService.getAllBooks()).thenReturn(books);
        when(bookMapper.toDtoList(books)).thenReturn(bookDTOs);

        List<BookDTO> result = bookController.getAllBooks();

        assertEquals(2, result.size());
        assertEquals("Book 1", result.get(0).getTitle());
        assertEquals("Book 2", result.get(1).getTitle());
        verify(bookService, times(1)).getAllBooks();
        verify(bookMapper, times(1)).toDtoList(books);
    }

    @Test
    void searchBooks_Success() {
        String searchTitle = "Test";

        Book book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");

        List<Book> books = Arrays.asList(book);

        BookDTO bookDTO = new BookDTO();
        bookDTO.setId(1L);
        bookDTO.setTitle("Test Book");

        List<BookDTO> bookDTOs = Arrays.asList(bookDTO);

        when(bookService.searchByTitle(searchTitle)).thenReturn(books);
        when(bookMapper.toDtoList(books)).thenReturn(bookDTOs);

        List<BookDTO> result = bookController.searchBooks(searchTitle);

        assertEquals(1, result.size());
        assertEquals("Test Book", result.get(0).getTitle());
        verify(bookService, times(1)).searchByTitle(searchTitle);
        verify(bookMapper, times(1)).toDtoList(books);
    }



    @Test
    void getInStockBooks_Success() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("In Stock Book");

        List<Book> books = Arrays.asList(book);

        BookDTO bookDTO = new BookDTO();
        bookDTO.setId(1L);
        bookDTO.setTitle("In Stock Book");

        List<BookDTO> bookDTOs = Arrays.asList(bookDTO);

        when(bookService.getBooksInStock()).thenReturn(books);
        when(bookMapper.toDtoList(books)).thenReturn(bookDTOs);

        List<BookDTO> result = bookController.getInStockBooks();

        assertEquals(1, result.size());
        assertEquals("In Stock Book", result.get(0).getTitle());
        verify(bookService, times(1)).getBooksInStock();
        verify(bookMapper, times(1)).toDtoList(books);
    }

    @Test
    void deleteBook_Success() {
        Long bookId = 1L;

        doNothing().when(bookService).deleteBook(bookId);

        ResponseEntity<Void> response = bookController.deleteBook(bookId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(bookService, times(1)).deleteBook(bookId);
    }
}