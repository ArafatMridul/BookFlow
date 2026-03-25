package com.example.bookflowproject.controllers;

import java.util.List;
import java.util.Optional;

import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @Test
    @WithMockUser(username = "reader", roles = {"USER"})
    @DisplayName("USER role should request return from /user/my-borrowings/{id}/request-return")
    void userShouldRequestReturn() throws Exception {
        Borrowing borrowing = borrowing(1L, "BORROWED", "reader");
        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(borrowing));

        mockMvc.perform(post("/user/my-borrowings/1/request-return").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

        @Test
        @WithMockUser(username = "reader", roles = {"USER"})
        @DisplayName("USER role should request borrow from /user/catalog/{id}/request-borrow")
        void userShouldRequestBorrow() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("reader");
        when(userRepository.findByUsername("reader")).thenReturn(Optional.of(user));

        Book book = new Book();
        book.setId(30L);
        book.setTitle("Refactoring");
        book.setAuthor("Martin Fowler");
        when(bookRepository.findById(30L)).thenReturn(Optional.of(book));
        when(borrowingRepository.existsByUserIdAndBookIdAndStatusIn(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyCollection()))
            .thenReturn(false);

        mockMvc.perform(post("/user/catalog/30/request-borrow").with(csrf()))
            .andExpect(status().is3xxRedirection());
        }

    @Test
    @WithMockUser(username = "reader", roles = {"USER"})
    @DisplayName("USER role should be forbidden from /librarian/borrowings/{id}/process-return")
    void userShouldBeForbiddenFromProcessReturn() throws Exception {
        mockMvc.perform(post("/librarian/borrowings/1/process-return").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("LIBRARIAN role should process return from /librarian/borrowings/{id}/process-return")
    void librarianShouldProcessReturn() throws Exception {
        Borrowing borrowing = borrowing(2L, "RETURN_REQUESTED", "reader");
        when(borrowingRepository.findById(2L)).thenReturn(Optional.of(borrowing));

        mockMvc.perform(post("/librarian/borrowings/2/process-return").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "reader", roles = {"USER"})
    @DisplayName("USER role should be forbidden from /librarian/borrowings/{id}/approve")
    void userShouldBeForbiddenFromApprove() throws Exception {
        mockMvc.perform(post("/librarian/borrowings/3/approve").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("LIBRARIAN role should approve borrow request from /librarian/borrowings/{id}/approve")
    void librarianShouldApproveBorrowRequest() throws Exception {
        Borrowing borrowing = borrowing(3L, "REQUESTED", "reader");
        borrowing.getBook().setAvailableCopies(2);
        when(borrowingRepository.findById(3L)).thenReturn(Optional.of(borrowing));

        mockMvc.perform(post("/librarian/borrowings/3/approve").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("LIBRARIAN role should reject borrow request from /librarian/borrowings/{id}/reject")
    void librarianShouldRejectBorrowRequest() throws Exception {
        Borrowing borrowing = borrowing(4L, "REQUESTED", "reader");
        when(borrowingRepository.findById(4L)).thenReturn(Optional.of(borrowing));

        mockMvc.perform(post("/librarian/borrowings/4/reject").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
    @DisplayName("LIBRARIAN role should reject return request from /librarian/borrowings/{id}/reject-return")
    void librarianShouldRejectReturnRequest() throws Exception {
        Borrowing borrowing = borrowing(5L, "RETURN_REQUESTED", "reader");
        when(borrowingRepository.findById(5L)).thenReturn(Optional.of(borrowing));

        mockMvc.perform(post("/librarian/borrowings/5/reject-return").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    private Borrowing borrowing(Long id, String status, String username) {
        User user = new User();
        user.setId(10L);
        user.setUsername(username);

        Book book = new Book();
        book.setId(20L);
        book.setTitle("Clean Code");
        book.setAuthor("Robert C. Martin");
        book.setAvailableCopies(1);
        book.setTotalCopies(3);

        Borrowing borrowing = new Borrowing();
        borrowing.setId(id);
        borrowing.setUser(user);
        borrowing.setBook(book);
        borrowing.setStatus(status);
        borrowing.setBorrowDate(java.time.LocalDate.now().minusDays(3));
        borrowing.setDueDate(java.time.LocalDate.now().plusDays(11));
        return borrowing;
    }
}
