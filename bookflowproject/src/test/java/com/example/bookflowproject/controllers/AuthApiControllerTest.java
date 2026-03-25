package com.example.bookflowproject.controllers;

import com.example.bookflowproject.controller.AuthApiController;
import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import com.example.bookflowproject.dto.LoginRequest;
import com.example.bookflowproject.dto.LoginResponse;
import com.example.bookflowproject.dto.SignupRequest;
import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.services.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("should return JWT token on valid login")
        void shouldReturnToken() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("admin");
            request.setPassword("admin123");

            LoginResponse response = new LoginResponse("mock-jwt", "Bearer", 1L, "admin", "admin@bookflow.com", "ADMIN");
            when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.username").value("admin"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("should return error on bad credentials")
        void shouldReturnErrorOnBadCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("admin");
            request.setPassword("wrong");

            when(authService.authenticateUser(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid username or password"));
        }

        @Test
        @DisplayName("should return error when account is disabled")
        void shouldReturnErrorWhenAccountIsDisabled() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("disabled-user");
            request.setPassword("password");

            when(authService.authenticateUser(any(LoginRequest.class)))
                    .thenThrow(new DisabledException("Account is disabled"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value("Your account is disabled. Please contact an administrator."));
        }

        @Test
        @DisplayName("should return 400 when request body fields are blank")
        void shouldReturn400OnBlankFields() throws Exception {
            LoginRequest request = new LoginRequest();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.usernameOrEmail").exists())
                    .andExpect(jsonPath("$.password").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/signup")
    class Signup {

        @Test
        @DisplayName("should return user DTO on successful signup")
        void shouldReturnUserOnSignup() throws Exception {
            SignupRequest request = new SignupRequest();
            request.setUsername("newuser");
            request.setEmail("new@bookflow.com");
            request.setPassword("Password1");
            request.setFirstName("New");
            request.setLastName("User");

            UserDTO userDTO = new UserDTO();
            userDTO.setId(1L);
            userDTO.setUsername("newuser");
            userDTO.setEmail("new@bookflow.com");
            userDTO.setRole("USER");

            when(authService.registerUser(any(SignupRequest.class))).thenReturn(userDTO);

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andExpect(jsonPath("$.email").value("new@bookflow.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }

        @Test
        @DisplayName("should return 400 on validation errors (missing fields)")
        void shouldReturn400OnValidationErrors() throws Exception {
            SignupRequest request = new SignupRequest();

            mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.password").exists());
        }
    }
}
