package com.example.bookflowproject.controllers;

import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.controller.AdminController;
import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Borrowing;
import com.example.bookflowproject.entity.Role;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminController adminController;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private BorrowingRepository borrowingRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /admin/users")
    class UsersPage {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturnUsersPageWithStats() throws Exception {
            when(userRepository.findAll()).thenReturn(List.of(
                    user(1L, "admin", "admin@bookflow.com", true, "ADMIN"),
                    user(2L, "librarian", "librarian@bookflow.com", true, "LIBRARIAN"),
                    user(3L, "reader", "reader@bookflow.com", false, "USER")
            ));

            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/users"))
                    .andExpect(model().attribute("username", "admin"))
                    .andExpect(model().attribute("totalUsers", 3))
                    .andExpect(model().attribute("adminCount", 1L))
                    .andExpect(model().attribute("librarianCount", 1L))
                    .andExpect(model().attribute("memberCount", 1L))
                    .andExpect(model().attribute("disabledCount", 1L));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldToggleUserEnabled() throws Exception {
            User reader = user(3L, "reader", "reader@bookflow.com", false, "USER");
            when(userRepository.findById(3L)).thenReturn(Optional.of(reader));

            mockMvc.perform(post("/admin/users/3/toggle-enabled"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users"))
                    .andExpect(flash().attribute("successMsg", "User account enabled."));

            verify(userRepository).save(reader);
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldShowErrorWhenToggleUserMissing() throws Exception {
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/admin/users/404/toggle-enabled"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users"))
                    .andExpect(flash().attribute("errorMsg", "User not found."));

            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        }

        @Test
        void shouldBlockSelfDisableAttempt() throws Exception {
            User admin = user(1L, "admin", "admin@bookflow.com", true, "ADMIN");
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
            String view = adminController.toggleUserEnabled(
                1L,
                new UsernamePasswordAuthenticationToken("admin", "password"),
                redirectAttributes
            );

            org.junit.jupiter.api.Assertions.assertEquals("redirect:/admin/users", view);
            org.junit.jupiter.api.Assertions.assertEquals(
                "You cannot disable your own admin account.",
                redirectAttributes.getFlashAttributes().get("errorMsg")
            );
            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldFilterUsersBySearchQuery() throws Exception {
            when(userRepository.findAll()).thenReturn(List.of(
                    user(1L, "admin", "admin@bookflow.com", true, "ADMIN"),
                    user(2L, "librarian", "librarian@bookflow.com", true, "LIBRARIAN"),
                    user(3L, "reader", "reader@bookflow.com", true, "USER")
            ));

            mockMvc.perform(get("/admin/users").param("search", "reader"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/users"))
                    .andExpect(model().attribute("search", "reader"))
                    .andExpect(model().attribute("totalUsers", 1));
        }
    }

    @Nested
    @DisplayName("GET /admin/reports")
    class ReportsPage {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturnReportsPageWithActivityMetrics() throws Exception {
            when(userRepository.count()).thenReturn(3L);
            when(userRepository.findAll()).thenReturn(List.of(
                    user(1L, "admin", "admin@bookflow.com", true, "ADMIN"),
                    user(2L, "librarian", "librarian@bookflow.com", true, "LIBRARIAN"),
                    user(3L, "reader", "reader@bookflow.com", true, "USER")
            ));
            when(bookRepository.count()).thenReturn(12L);
            when(borrowingRepository.countByStatus("REQUESTED")).thenReturn(4L);
            when(borrowingRepository.countByStatus("RETURN_REQUESTED")).thenReturn(2L);
            when(borrowingRepository.countByStatus("BORROWED")).thenReturn(5L);
            when(borrowingRepository.countByStatus("OVERDUE")).thenReturn(1L);
            when(borrowingRepository.countByStatus("RETURNED")).thenReturn(9L);
            when(borrowingRepository.countByStatus("REJECTED")).thenReturn(3L);
            when(borrowingRepository.findByStatusInOrderByBorrowDateDesc(anyCollection())).thenReturn(List.of(
                    borrowing("Book One", "REQUESTED"),
                    borrowing("Book Two", "RETURN_REQUESTED")
            ));

            mockMvc.perform(get("/admin/reports"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/reports"))
                    .andExpect(model().attribute("username", "admin"))
                    .andExpect(model().attribute("totalUsers", 3L))
                    .andExpect(model().attribute("totalBooks", 12L))
                    .andExpect(model().attribute("totalLibrarians", 1L))
                    .andExpect(model().attribute("pendingBorrowRequests", 4L))
                    .andExpect(model().attribute("pendingReturnRequests", 2L))
                    .andExpect(model().attribute("activeLoans", 5L))
                    .andExpect(model().attribute("overdueLoans", 1L));
        }
    }

    @Nested
    @DisplayName("GET /admin/books")
    class BooksPage {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturnBookOverviewPage() throws Exception {
            when(bookRepository.findAll()).thenReturn(List.of(
                    book(1L, "Effective Java", "Joshua Bloch", 3, 5),
                    book(2L, "Clean Code", "Robert C. Martin", 1, 3)
            ));

            mockMvc.perform(get("/admin/books"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("admin/books"))
                    .andExpect(model().attribute("username", "admin"))
                    .andExpect(model().attribute("lowStockBooks", 1L))
                    .andExpect(model().attribute("availableBooks", 2L));
        }

                @Test
                @WithMockUser(username = "admin", roles = {"ADMIN"})
                void shouldUseSearchOnBooksPage() throws Exception {
                    when(bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase("code", "code"))
                        .thenReturn(List.of(book(2L, "Clean Code", "Robert C. Martin", 1, 3)));

                    mockMvc.perform(get("/admin/books").param("search", "code"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("admin/books"))
                        .andExpect(model().attribute("search", "code"))
                        .andExpect(model().attribute("lowStockBooks", 1L))
                        .andExpect(model().attribute("availableBooks", 1L));
                }
    }

    private User user(Long id, String username, String email, boolean enabled, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setEnabled(enabled);
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    private Book book(Long id, String title, String author, int availableCopies, int totalCopies) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setAvailableCopies(availableCopies);
        book.setTotalCopies(totalCopies);
        return book;
    }

    private Borrowing borrowing(String bookTitle, String status) {
        User user = new User();
        user.setUsername("reader");
        Book book = new Book();
        book.setTitle(bookTitle);
        Borrowing borrowing = new Borrowing();
        borrowing.setUser(user);
        borrowing.setBook(book);
        borrowing.setStatus(status);
        return borrowing;
    }
}




