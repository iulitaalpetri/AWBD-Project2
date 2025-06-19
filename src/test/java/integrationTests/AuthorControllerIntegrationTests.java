package integrationTests;


import com.awbd.bookstore.DTOs.AuthorDTO;
import com.awbd.bookstore.controllers.AuthorController;
import com.awbd.bookstore.exceptions.author.AuthorAlreadyExistsException;
import com.awbd.bookstore.exceptions.author.AuthorNotFoundException;
import com.awbd.bookstore.mappers.AuthorMapper;
import com.awbd.bookstore.models.Author;
import com.awbd.bookstore.services.AuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthorControllerIntegrationTests {

    @Mock
    private AuthorService authorService;

    @Mock
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorController authorController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(authorController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Pentru LocalDate serialization
    }

    // ==================== CREATE AUTHOR TESTS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateAuthor_Success() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO("George Orwell", "British author", LocalDate.of(1903, 6, 25));
        Author authorEntity = new Author("George Orwell", "British author", LocalDate.of(1903, 6, 25));
        Author createdAuthor = new Author("George Orwell", "British author", LocalDate.of(1903, 6, 25));
        createdAuthor.setId(1L);

        when(authorMapper.toEntity(any(AuthorDTO.class))).thenReturn(authorEntity);
        when(authorService.create(any(Author.class))).thenReturn(createdAuthor);

        // Act & Assert
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/authors/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("George Orwell"))
                .andExpect(jsonPath("$.biography").value("British author"));

        verify(authorMapper, times(1)).toEntity(any(AuthorDTO.class));
        verify(authorService, times(1)).create(any(Author.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testCreateAuthor_Forbidden_NonAdmin() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO("George Orwell", "British author", LocalDate.of(1903, 6, 25));

        // Act & Assert
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authorService, never()).create(any(Author.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateAuthor_AlreadyExists() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO("George Orwell", "British author", LocalDate.of(1903, 6, 25));
        Author authorEntity = new Author("George Orwell", "British author", LocalDate.of(1903, 6, 25));

        when(authorMapper.toEntity(any(AuthorDTO.class))).thenReturn(authorEntity);
        when(authorService.create(any(Author.class)))
                .thenThrow(new AuthorAlreadyExistsException("Author already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Author already exists"));

        verify(authorService, times(1)).create(any(Author.class));
    }

    // ==================== GET AUTHOR BY ID TESTS ====================

    @Test
    public void testGetAuthorById_Success() throws Exception {
        // Arrange
        Author author = new Author("George Orwell", "British author", LocalDate.of(1903, 6, 25));
        author.setId(1L);

        when(authorService.getById(1L)).thenReturn(author);

        // Act & Assert
        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("George Orwell"))
                .andExpect(jsonPath("$.biography").value("British author"));

        verify(authorService, times(1)).getById(1L);
    }

    @Test
    public void testGetAuthorById_NotFound() throws Exception {
        // Arrange
        when(authorService.getById(999L))
                .thenThrow(new AuthorNotFoundException("Author with ID 999 not found"));

        // Act & Assert
        mockMvc.perform(get("/api/authors/999"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Author with ID 999 not found"));

        verify(authorService, times(1)).getById(999L);
    }

    // ==================== GET ALL AUTHORS TESTS ====================

    @Test
    public void testGetAllAuthors_Success() throws Exception {
        // Arrange
        Author author1 = new Author("George Orwell", "British author", LocalDate.of(1903, 6, 25));
        author1.setId(1L);
        Author author2 = new Author("Jane Austen", "English novelist", LocalDate.of(1775, 12, 16));
        author2.setId(2L);

        List<Author> authors = Arrays.asList(author1, author2);
        when(authorService.getAll()).thenReturn(authors);

        // Act & Assert
        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("George Orwell"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Jane Austen"));

        verify(authorService, times(1)).getAll();
    }

    @Test
    public void testGetAllAuthors_EmptyList() throws Exception {
        // Arrange
        when(authorService.getAll()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(authorService, times(1)).getAll();
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateAuthor_Success() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO(1L, "George Orwell Updated", "Updated biography", LocalDate.of(1903, 6, 25));
        Author authorEntity = new Author("George Orwell Updated", "Updated biography", LocalDate.of(1903, 6, 25));
        Author updatedAuthor = new Author("George Orwell Updated", "Updated biography", LocalDate.of(1903, 6, 25));
        updatedAuthor.setId(1L);

        when(authorMapper.toEntity(any(AuthorDTO.class))).thenReturn(authorEntity);
        when(authorService.update(eq(1L), any(Author.class))).thenReturn(updatedAuthor);

        // Act & Assert
        mockMvc.perform(put("/api/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("George Orwell Updated"))
                .andExpect(jsonPath("$.biography").value("Updated biography"));

        verify(authorMapper, times(1)).toEntity(any(AuthorDTO.class));
        verify(authorService, times(1)).update(eq(1L), any(Author.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateAuthor_IdMismatch() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO(2L, "George Orwell", "British author", LocalDate.of(1903, 6, 25));

        // Act & Assert
        mockMvc.perform(put("/api/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Id from path does not match with id from request"));

        verify(authorService, never()).update(anyLong(), any(Author.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUpdateAuthor_Forbidden_NonAdmin() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO(1L, "George Orwell", "British author", LocalDate.of(1903, 6, 25));

        // Act & Assert
        mockMvc.perform(put("/api/authors/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authorService, never()).update(anyLong(), any(Author.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateAuthor_NotFound() throws Exception {
        // Arrange
        AuthorDTO authorDTO = new AuthorDTO(999L, "George Orwell", "British author", LocalDate.of(1903, 6, 25));
        Author authorEntity = new Author("George Orwell", "British author", LocalDate.of(1903, 6, 25));

        when(authorMapper.toEntity(any(AuthorDTO.class))).thenReturn(authorEntity);
        when(authorService.update(eq(999L), any(Author.class)))
                .thenThrow(new AuthorNotFoundException("Author with ID 999 not found"));

        // Act & Assert
        mockMvc.perform(put("/api/authors/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorDTO))
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Author with ID 999 not found"));

        verify(authorService, times(1)).update(eq(999L), any(Author.class));
    }

    // ==================== DELETE AUTHOR TESTS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteAuthor_Success() throws Exception {
        // Arrange
        doNothing().when(authorService).delete(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/authors/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(authorService, times(1)).delete(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testDeleteAuthor_Forbidden_NonAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/authors/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authorService, never()).delete(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testDeleteAuthor_NotFound() throws Exception {
        // Arrange
        doThrow(new AuthorNotFoundException("Author with ID 999 not found"))
                .when(authorService).delete(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/authors/999")
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Author with ID 999 not found"));

        verify(authorService, times(1)).delete(999L);
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testCreateAuthor_InvalidData() throws Exception {
        // Arrange - author without name (assuming name is required based on @Column(nullable = false))
        AuthorDTO invalidAuthorDTO = new AuthorDTO(null, "Biography", LocalDate.of(1903, 6, 25));

        // Act & Assert
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAuthorDTO))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authorService, never()).create(any(Author.class));
    }
}