package com.example.bookflowproject.controllers;

import com.example.bookflowproject.controller.UserController;
import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;
import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProperties jwtProperties;

    private UserDTO testUserDTO;
    private UserDTO adminUserDTO;

    @BeforeEach
    void setUp() {
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setEmail("test@bookflow.com");
        testUserDTO.setFirstName("Test");
        testUserDTO.setLastName("User");
        testUserDTO.setRole("USER");

        adminUserDTO = new UserDTO();
        adminUserDTO.setId(2L);
        adminUserDTO.setUsername("admin");
        adminUserDTO.setEmail("admin@bookflow.com");
        adminUserDTO.setFirstName("Admin");
        adminUserDTO.setLastName("User");
        adminUserDTO.setRole("ADMIN");
    }

    @Nested
    @DisplayName("GET /api/user/profile")
    class GetProfile {

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return current user profile")
        void shouldReturnCurrentUser() throws Exception {
            when(userService.getCurrentUser()).thenReturn(testUserDTO);

            mockMvc.perform(get("/api/user/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@bookflow.com"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/{id}")
    class GetUserById {

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should return user by id for admin")
        void shouldReturnUserForAdmin() throws Exception {
            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            mockMvc.perform(get("/api/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @WithMockUser(roles = {"LIBRARIAN"})
        @DisplayName("should return user by id for librarian")
        void shouldReturnUserForLibrarian() throws Exception {
            when(userService.getUserById(1L)).thenReturn(testUserDTO);

            mockMvc.perform(get("/api/user/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @WithMockUser(roles = {"USER"})
        @DisplayName("should return forbidden for regular user")
        void shouldReturnForbiddenForRegularUser() throws Exception {
            mockMvc.perform(get("/api/user/1"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/user")
    class GetAllUsers {

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should return all users for admin")
        void shouldReturnAllUsersForAdmin() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(testUserDTO, adminUserDTO));

            mockMvc.perform(get("/api/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].username").value("testuser"))
                    .andExpect(jsonPath("$[1].username").value("admin"));
        }

        @Test
        @WithMockUser(roles = {"ADMIN"})
        @DisplayName("should return empty list when no users")
        void shouldReturnEmptyList() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @WithMockUser(roles = {"LIBRARIAN"})
        @DisplayName("should return forbidden for librarian")
        void shouldReturnForbiddenForLibrarian() throws Exception {
            mockMvc.perform(get("/api/user"))
                    .andExpect(status().isForbidden());
        }
    }
}
