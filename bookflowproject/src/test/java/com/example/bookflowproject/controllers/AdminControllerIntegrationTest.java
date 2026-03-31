package com.example.bookflowproject.controllers;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.bookflowproject.config.JwtProperties;
import com.example.bookflowproject.entity.Book;
import com.example.bookflowproject.entity.Role;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.repository.BookRepository;
import com.example.bookflowproject.repository.BorrowingRepository;
import com.example.bookflowproject.repository.UserRepository;
import com.example.bookflowproject.security.JwtAuthenticationFilter;
import com.example.bookflowproject.security.JwtTokenProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

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

    @Test
    @DisplayName("anonymous request to /admin/users should redirect to login")
    void anonymousUsersPageShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("anonymous request to /admin/reports should redirect to login")
    void anonymousReportsPageShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("anonymous request to /admin/books should redirect to login")
    void anonymousBooksPageShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    @DisplayName("USER role should be forbidden from /admin/reports")
    void userRoleShouldBeForbiddenFromReports() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("USER role should be forbidden from /admin/users")
    void userRoleShouldBeForbiddenFromUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("USER role should be forbidden from /admin/books")
    void userRoleShouldBeForbiddenFromBooks() throws Exception {
        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN role should access /admin/users")
    void adminRoleShouldAccessUsersPage() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(adminUser("admin"), memberUser("reader", true)));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN role should access /admin/reports")
    void adminRoleShouldAccessReportsPage() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of(adminUser("admin"), memberUser("reader", true)));
        when(borrowingRepository.findByStatusInOrderByBorrowDateDesc(any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN role should access /admin/books")
    void adminRoleShouldAccessBooksPage() throws Exception {
        when(bookRepository.findAll()).thenReturn(List.of(book()));

        mockMvc.perform(get("/admin/books"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/books"));
    }

    @Test
    @DisplayName("anonymous request to POST /admin/users/{id}/toggle-enabled should redirect to login")
    void anonymousToggleEnabledShouldRedirectToLogin() throws Exception {
        mockMvc.perform(post("/admin/users/1/toggle-enabled").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    @DisplayName("USER role should be forbidden from POST /admin/users/{id}/toggle-enabled")
    void userRoleShouldBeForbiddenFromToggleEnabled() throws Exception {
        mockMvc.perform(post("/admin/users/1/toggle-enabled").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN should toggle another user account")
    void adminShouldToggleAnotherUserAccount() throws Exception {
        User target = memberUser("reader", true);
        target.setId(2L);
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(target));

        mockMvc.perform(post("/admin/users/2/toggle-enabled").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMsg", "User account disabled."));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("toggle-enabled should return error when user is missing")
    void toggleEnabledShouldHandleMissingUser() throws Exception {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(post("/admin/users/99/toggle-enabled").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorMsg", "User not found."));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("toggle-enabled should block self-disable")
    void toggleEnabledShouldBlockSelfDisable() throws Exception {
        User self = adminUser("admin");
        self.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(self));

        mockMvc.perform(post("/admin/users/1/toggle-enabled").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorMsg", "You cannot disable your own admin account."));

        verify(userRepository, never()).save(any(User.class));
    }

    private Book book() {
        Book book = new Book();
        book.setId(1L);
        book.setTitle("Effective Java");
        book.setAuthor("Joshua Bloch");
        book.setAvailableCopies(3);
        book.setTotalCopies(5);
        return book;
    }

    private User adminUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@bookflow.com");
        user.setEnabled(true);

        Role role = new Role();
        role.setName("ADMIN");
        user.setRoles(Set.of(role));
        return user;
    }

    private User memberUser(String username, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@bookflow.com");
        user.setEnabled(enabled);

        Role role = new Role();
        role.setName("USER");
        user.setRoles(Set.of(role));
        return user;
    }
}


