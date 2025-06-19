package com.awbd.bookstore.unitTests;

import com.awbd.bookstore.DTOs.AuthorDTO;
import com.awbd.bookstore.controllers.AuthorController;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.services.AuthorService;
import com.awbd.bookstore.mappers.AuthorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorControllerTest {

    @Mock
    private AuthorService authorService;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorController authorController;

    private Author author;
    private AuthorDTO authorDTO;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("Test Author");
        author.setBiography("Test Biography");
        author.setBirthDate(LocalDate.of(1980, 5, 15));

        authorDTO = new AuthorDTO();
        authorDTO.setId(1L);
        authorDTO.setName("Test Author");
        authorDTO.setBiography("Test Biography");
        authorDTO.setBirthDate(LocalDate.of(1980, 5, 15));
    }

    @Test
    void createAuthor_Success() {
        Author createdAuthor = new Author();
        createdAuthor.setId(1L);
        createdAuthor.setName("Test Author");

        when(authorMapper.toEntity(authorDTO)).thenReturn(author);
        when(authorService.create(author)).thenReturn(createdAuthor);

        ResponseEntity<Author> result = authorController.createAuthor(authorDTO);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(createdAuthor, result.getBody());
        assertEquals(URI.create("/api/authors/1"), result.getHeaders().getLocation());
        verify(authorMapper, times(1)).toEntity(authorDTO);
        verify(authorService, times(1)).create(author);
    }

//    @Test
//    void getAuthorById_Success() {
//        when(authorService.getById(1L)).thenReturn(author);
//
//        ResponseEntity<Author> result = authorController.getAuthorById(1L);
//
//        assertEquals(HttpStatus.OK, result.getStatusCode());
//        assertEquals(author, result.getBody());
//        verify(authorService, times(1)).getById(1L);
//    }

//    @Test
//    void getAllAuthors_Success() {
//        List<Author> authors = Arrays.asList(author);
//
//        when(authorService.getAll()).thenReturn(authors);
//
//        List<Author> result = authorController.getAllAuthors();
//
//        assertEquals(1, result.size());
//        assertEquals("Test Author", result.get(0).getName());
//        verify(authorService, times(1)).getAll();
//    }

    @Test
    void updateAuthor_Success() {
        Author updatedAuthor = new Author();
        updatedAuthor.setId(1L);
        updatedAuthor.setName("Updated Author");

        when(authorMapper.toEntity(authorDTO)).thenReturn(author);
        when(authorService.update(1L, author)).thenReturn(updatedAuthor);

        ResponseEntity<Author> result = authorController.updateAuthor(1L, authorDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(updatedAuthor, result.getBody());
        verify(authorMapper, times(1)).toEntity(authorDTO);
        verify(authorService, times(1)).update(1L, author);
    }

    @Test
    void deleteAuthor_Success() {
        doNothing().when(authorService).delete(1L);

        ResponseEntity<Void> result = authorController.deleteAuthor(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        assertNull(result.getBody());
        verify(authorService, times(1)).delete(1L);
    }
}