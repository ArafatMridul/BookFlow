package com.example.bookflowproject.controllers;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private BorrowingRepository borrowingRepository;

    @MockitoBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("reader");
        when(bookRepository.findAll()).thenReturn(List.of());
        when(borrowingRepository.findByStatusOrderByBorrowDateDesc("REQUESTED")).thenReturn(List.of());
        when(borrowingRepository.findByStatusOrderByBorrowDateDesc("BORROWED")).thenReturn(List.of());
        when(borrowingRepository.findByUserUsernameOrderByBorrowDateDesc("reader")).thenReturn(List.of());
        when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("anonymous request to /user/catalog should redirect to login")
    void anonymousCatalogShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/user/catalog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "reader", roles = {"USER"})
    @DisplayName("USER role should access /user/catalog")
    void userShouldAccessCatalog() throws Exception {
        mockMvc.perform(get("/user/catalog"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "reader", roles = {"USER"})
    @DisplayName("USER role should be forbidden from /librarian/borrowings")
    void userShouldBeForbiddenFromLibrarianBorrowings() throws Exception {
        mockMvc.perform(get("/librarian/borrowings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("LIBRARIAN role should access /librarian/borrowings")
    void librarianShouldAccessBorrowings() throws Exception {
        mockMvc.perform(get("/librarian/borrowings"))
                .andExpect(status().isOk());
    }
}
