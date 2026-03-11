package com.example.bookflowproject.services;

import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.entity.Role;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.error.ResourceNotFoundException;
import com.example.bookflowproject.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@bookflow.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        testUser.setRoles(new HashSet<>(Set.of(userRole)));

        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@bookflow.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setEnabled(true);
        adminUser.setRoles(new HashSet<>(Set.of(adminRole)));
    }

    private void mockSecurityContext(String username) {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(username);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("getCurrentUser()")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current authenticated user")
        void shouldReturnCurrentUser() {
            mockSecurityContext("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDTO result = userService.getCurrentUser();

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getEmail()).isEqualTo("test@bookflow.com");
            assertThat(result.getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("should throw when authenticated user not found in DB")
        void shouldThrowWhenUserNotInDb() {
            mockSecurityContext("ghost");
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getCurrentUser())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ghost");
        }
    }

    @Nested
    @DisplayName("getCurrentUserEntity()")
    class GetCurrentUserEntity {

        @Test
        @DisplayName("should return current user entity")
        void shouldReturnCurrentUserEntity() {
            mockSecurityContext("testuser");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            User result = userService.getCurrentUserEntity();

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("should return user by id")
        void shouldReturnUserById() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserDTO result = userService.getUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should throw when user not found by id")
        void shouldThrowWhenNotFoundById() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("getUserByUsername()")
    class GetUserByUsername {

        @Test
        @DisplayName("should return user by username")
        void shouldReturnUserByUsername() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            UserDTO result = userService.getUserByUsername("testuser");

            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getFirstName()).isEqualTo("Test");
        }

        @Test
        @DisplayName("should throw when username not found")
        void shouldThrowWhenUsernameNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserByEmail()")
    class GetUserByEmail {

        @Test
        @DisplayName("should return user by email")
        void shouldReturnUserByEmail() {
            when(userRepository.findByEmail("test@bookflow.com")).thenReturn(Optional.of(testUser));

            UserDTO result = userService.getUserByEmail("test@bookflow.com");

            assertThat(result.getEmail()).isEqualTo("test@bookflow.com");
        }

        @Test
        @DisplayName("should throw when email not found")
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmail("nope@bookflow.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("nope@bookflow.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() {
            when(userRepository.findAll()).thenReturn(List.of(testUser, adminUser));

            List<UserDTO> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("testuser");
            assertThat(result.get(1).getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("should return empty list when no users exist")
        void shouldReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserDTO> result = userService.getAllUsers();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("should update user fields")
        void shouldUpdateUserFields() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserDTO updateDto = new UserDTO();
            updateDto.setFirstName("Updated");
            updateDto.setLastName("Name");
            updateDto.setEmail("updated@bookflow.com");

            when(userRepository.existsByEmail("updated@bookflow.com")).thenReturn(false);

            UserDTO result = userService.updateUser(1L, updateDto);

            assertThat(result).isNotNull();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should throw when updating with duplicate email")
        void shouldThrowOnDuplicateEmail() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            UserDTO updateDto = new UserDTO();
            updateDto.setEmail("existing@bookflow.com");

            when(userRepository.existsByEmail("existing@bookflow.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, updateDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Email is already in use");
        }

        @Test
        @DisplayName("should not change email if same as current")
        void shouldNotCheckEmailIfSame() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            UserDTO updateDto = new UserDTO();
            updateDto.setEmail("test@bookflow.com"); // same as current

            UserDTO result = userService.updateUser(1L, updateDto);

            assertThat(result).isNotNull();
            verify(userRepository, never()).existsByEmail(anyString());
        }

        @Test
        @DisplayName("should throw when user not found for update")
        void shouldThrowWhenUserNotFoundForUpdate() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(999L, new UserDTO()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("should delete user by id")
        void shouldDeleteUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            userService.deleteUser(1L);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("should throw when user not found for deletion")
        void shouldThrowWhenUserNotFoundForDelete() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("existsByUsername() / existsByEmail()")
    class ExistsMethods {

        @Test
        @DisplayName("should return true when username exists")
        void shouldReturnTrueForExistingUsername() {
            when(userRepository.existsByUsername("testuser")).thenReturn(true);
            assertThat(userService.existsByUsername("testuser")).isTrue();
        }

        @Test
        @DisplayName("should return false when username does not exist")
        void shouldReturnFalseForNonExistingUsername() {
            when(userRepository.existsByUsername("nobody")).thenReturn(false);
            assertThat(userService.existsByUsername("nobody")).isFalse();
        }

        @Test
        @DisplayName("should return true when email exists")
        void shouldReturnTrueForExistingEmail() {
            when(userRepository.existsByEmail("test@bookflow.com")).thenReturn(true);
            assertThat(userService.existsByEmail("test@bookflow.com")).isTrue();
        }

        @Test
        @DisplayName("should return false when email does not exist")
        void shouldReturnFalseForNonExistingEmail() {
            when(userRepository.existsByEmail("nope@bookflow.com")).thenReturn(false);
            assertThat(userService.existsByEmail("nope@bookflow.com")).isFalse();
        }
    }
}

