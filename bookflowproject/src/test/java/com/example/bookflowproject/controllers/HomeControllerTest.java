package com.example.bookflowproject.controllers;

import com.example.bookflowproject.controller.HomeController;
import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Nested
    @DisplayName("GET /")
    class HomePage {

        @Test
        @DisplayName("should return index page")
        void shouldReturnIndexPage() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"));
        }
    }

    @Nested
    @DisplayName("GET /dashboard")
    class DashboardRedirect {

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should redirect admin to /admin/dashboard")
        void shouldRedirectAdmin() throws Exception {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/dashboard"));
        }

        @Test
        @WithMockUser(roles = {"LIBRARIAN"})
        @DisplayName("should redirect librarian to /librarian/dashboard")
        void shouldRedirectLibrarian() throws Exception {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/librarian/dashboard"));
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should redirect user to /user/dashboard")
        void shouldRedirectUser() throws Exception {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/user/dashboard"));
        }

        @Test
        @DisplayName("should redirect to /login when authentication is null")
        void shouldRedirectToLoginWhenNoAuth() throws Exception {
            mockMvc.perform(get("/dashboard"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }

    @Nested
    @DisplayName("GET /profile")
    class ProfilePage {

        @Test
        @DisplayName("should return profile page")
        void shouldReturnProfilePage() throws Exception {
            mockMvc.perform(get("/profile"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("profile"));
        }
    }
}
