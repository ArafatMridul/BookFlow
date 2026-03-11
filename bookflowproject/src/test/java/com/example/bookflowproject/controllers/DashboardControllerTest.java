package com.example.bookflowproject.controllers;

import com.example.bookflowproject.controller.DashboardController;
import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @DisplayName("GET /admin/dashboard")
    class AdminDashboard {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("should return admin dashboard with stats")
        void shouldReturnAdminDashboard() throws Exception {
            when(userRepository.count()).thenReturn(10L);
            when(bookRepository.count()).thenReturn(50L);
            when(borrowingRepository.countByStatus("BORROWED")).thenReturn(5L);
            when(borrowingRepository.countByStatus("OVERDUE")).thenReturn(2L);

            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/admin"))
                    .andExpect(model().attribute("username", "admin"))
                    .andExpect(model().attribute("totalUsers", 10L))
                    .andExpect(model().attribute("totalBooks", 50L))
                    .andExpect(model().attribute("activeLoans", 5L))
                    .andExpect(model().attribute("overdueReturns", 2L));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("should show zero stats when database is empty")
        void shouldShowZeroStats() throws Exception {
            when(userRepository.count()).thenReturn(0L);
            when(bookRepository.count()).thenReturn(0L);
            when(borrowingRepository.countByStatus("BORROWED")).thenReturn(0L);
            when(borrowingRepository.countByStatus("OVERDUE")).thenReturn(0L);

            mockMvc.perform(get("/admin/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("totalUsers", 0L))
                    .andExpect(model().attribute("totalBooks", 0L));
        }
    }

    @Nested
    @DisplayName("GET /librarian/dashboard")
    class LibrarianDashboard {

        @Test
        @WithMockUser(username = "librarian", roles = {"LIBRARIAN"})
        @DisplayName("should return librarian dashboard with stats")
        void shouldReturnLibrarianDashboard() throws Exception {
            when(bookRepository.count()).thenReturn(50L);
            when(borrowingRepository.countByStatus("BORROWED")).thenReturn(8L);
            when(borrowingRepository.countByStatus("OVERDUE")).thenReturn(1L);

            mockMvc.perform(get("/librarian/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/librarian"))
                    .andExpect(model().attribute("username", "librarian"))
                    .andExpect(model().attribute("totalBooks", 50L))
                    .andExpect(model().attribute("currentlyBorrowed", 8L))
                    .andExpect(model().attribute("overdueReturns", 1L));
        }
    }

    @Nested
    @DisplayName("GET /user/dashboard")
    class UserDashboard {

        @Test
        @WithMockUser(username = "john", roles = {"USER"})
        @DisplayName("should return user dashboard with personal stats")
        void shouldReturnUserDashboard() throws Exception {
            when(bookRepository.count()).thenReturn(50L);
            when(borrowingRepository.countByUserUsernameAndStatus("john", "BORROWED")).thenReturn(3L);
            when(borrowingRepository.countByUserUsernameAndStatus("john", "RETURNED")).thenReturn(7L);

            mockMvc.perform(get("/user/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("dashboard/user"))
                    .andExpect(model().attribute("username", "john"))
                    .andExpect(model().attribute("totalBooks", 50L))
                    .andExpect(model().attribute("myBorrowings", 3L))
                    .andExpect(model().attribute("myReturned", 7L));
        }

        @Test
        @WithMockUser(username = "jane", roles = {"USER"})
        @DisplayName("should return zero borrowings for new user")
        void shouldReturnZeroBorrowingsForNewUser() throws Exception {
            when(bookRepository.count()).thenReturn(10L);
            when(borrowingRepository.countByUserUsernameAndStatus("jane", "BORROWED")).thenReturn(0L);
            when(borrowingRepository.countByUserUsernameAndStatus("jane", "RETURNED")).thenReturn(0L);

            mockMvc.perform(get("/user/dashboard"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("myBorrowings", 0L))
                    .andExpect(model().attribute("myReturned", 0L));
        }
    }
}
