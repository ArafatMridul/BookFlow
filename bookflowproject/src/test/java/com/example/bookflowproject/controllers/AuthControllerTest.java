package com.example.bookflowproject.controllers;

import com.example.bookflowproject.controller.AuthController;
import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import com.example.bookflowproject.dto.SignupRequest;
import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.error.BadRequestException;
import com.example.bookflowproject.services.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Nested
    @DisplayName("GET /login")
    class LoginPage {

        @Test
        @DisplayName("should return login page")
        void shouldReturnLoginPage() throws Exception {
            mockMvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/login"));
        }

        @Test
        @DisplayName("should show error message when error param present")
        void shouldShowErrorMessage() throws Exception {
            mockMvc.perform(get("/login").param("error", "true"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/login"))
                    .andExpect(model().attribute("error", "Invalid username or password"));
        }

        @Test
        @DisplayName("should show disabled-account message when error indicates disabled user")
        void shouldShowDisabledAccountMessage() throws Exception {
            mockMvc.perform(get("/login").param("error", "disabled"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/login"))
                    .andExpect(model().attribute("error", "Your account is disabled. Please contact an administrator."));
        }

        @Test
        @DisplayName("should show success message when registered param present")
        void shouldShowSuccessMessage() throws Exception {
            mockMvc.perform(get("/login").param("registered", "true"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/login"))
                    .andExpect(model().attribute("success", "Registration successful! Please login."));
        }
    }

    @Nested
    @DisplayName("GET /register")
    class RegisterPage {

        @Test
        @DisplayName("should return register page with empty form")
        void shouldReturnRegisterPage() throws Exception {
            mockMvc.perform(get("/register"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/register"))
                    .andExpect(model().attributeExists("signupRequest"));
        }
    }

    @Nested
    @DisplayName("POST /register")
    class RegisterUser {

        @Test
        @DisplayName("should redirect to login on successful registration")
        void shouldRedirectOnSuccess() throws Exception {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(1L);
            userDTO.setUsername("newuser");
            when(authService.registerUser(any(SignupRequest.class))).thenReturn(userDTO);

            mockMvc.perform(post("/register")
                            .param("username", "newuser")
                            .param("email", "new@bookflow.com")
                            .param("password", "Password1")
                            .param("firstName", "New")
                            .param("lastName", "User"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?registered=true"));

            verify(authService).registerUser(any(SignupRequest.class));
        }

        @Test
        @DisplayName("should return register page with error on duplicate username")
        void shouldReturnErrorOnDuplicate() throws Exception {
            when(authService.registerUser(any(SignupRequest.class)))
                    .thenThrow(new BadRequestException("Username is already taken"));

            mockMvc.perform(post("/register")
                            .param("username", "existing")
                            .param("email", "new@bookflow.com")
                            .param("password", "Password1")
                            .param("firstName", "New")
                            .param("lastName", "User"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("auth/register"))
                    .andExpect(model().attribute("error", "Username is already taken"));
        }
    }
}
