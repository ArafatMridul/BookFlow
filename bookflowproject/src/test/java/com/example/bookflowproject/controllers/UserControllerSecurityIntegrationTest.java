package com.example.bookflowproject.controllers;

import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("anonymous request to /api/user/profile should redirect to login")
    void anonymousProfileShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    @DisplayName("USER role should be forbidden from /api/user/{id}")
    void userRoleShouldBeForbiddenForGetUserById() throws Exception {
        mockMvc.perform(get("/api/user/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    @DisplayName("LIBRARIAN role should be forbidden from /api/user")
    void librarianRoleShouldBeForbiddenForGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("ADMIN role should access /api/user")
    void adminRoleShouldAccessGetAllUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(new UserDTO()));

        mockMvc.perform(get("/api/user"))
                .andExpect(status().isOk());
    }
}

