package com.example.bookflowproject.services;

import com.example.bookflowproject.dto.LoginRequest;
import com.example.bookflowproject.dto.LoginResponse;
import com.example.bookflowproject.dto.SignupRequest;
import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.entity.Role;
import com.example.bookflowproject.entity.User;
import com.example.bookflowproject.error.BadRequestException;
import com.example.bookflowproject.repository.RoleRepository;
import com.example.bookflowproject.repository.UserRepository;
import com.example.bookflowproject.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@bookflow.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRoles(new HashSet<>(Set.of(userRole)));
    }

    @Nested
    @DisplayName("authenticateUser()")
    class AuthenticateUser {

        @Test
        @DisplayName("should return LoginResponse with JWT token on valid credentials")
        void shouldReturnTokenOnValidCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("password123");

            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(tokenProvider.generateToken(authentication)).thenReturn("mock-jwt-token");
            when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                    .thenReturn(Optional.of(testUser));

            LoginResponse response = authService.authenticateUser(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("mock-jwt-token");
            assertThat(response.getType()).isEqualTo("Bearer");
            assertThat(response.getUsername()).isEqualTo("testuser");
            assertThat(response.getEmail()).isEqualTo("test@bookflow.com");
            assertThat(response.getRole()).isEqualTo("USER");
            assertThat(response.getId()).isEqualTo(1L);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(tokenProvider).generateToken(authentication);
        }

        @Test
        @DisplayName("should throw on invalid credentials")
        void shouldThrowOnInvalidCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("testuser");
            request.setPassword("wrongpassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.authenticateUser(request))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("should throw when user not found after auth")
        void shouldThrowWhenUserNotFoundAfterAuth() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("ghost");
            request.setPassword("password123");

            Authentication authentication = mock(Authentication.class);
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(tokenProvider.generateToken(authentication)).thenReturn("token");
            when(userRepository.findByUsernameOrEmail("ghost", "ghost"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.authenticateUser(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        private SignupRequest signupRequest;

        @BeforeEach
        void setUp() {
            signupRequest = new SignupRequest();
            signupRequest.setUsername("newuser");
            signupRequest.setEmail("new@bookflow.com");
            signupRequest.setPassword("Password1");
            signupRequest.setFirstName("New");
            signupRequest.setLastName("User");
        }

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@bookflow.com")).thenReturn(false);
            when(passwordEncoder.encode("Password1")).thenReturn("encodedPassword");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));

            User savedUser = new User();
            savedUser.setId(2L);
            savedUser.setUsername("newuser");
            savedUser.setEmail("new@bookflow.com");
            savedUser.setFirstName("New");
            savedUser.setLastName("User");
            savedUser.setRoles(new HashSet<>(Set.of(userRole)));
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            UserDTO result = authService.registerUser(signupRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getUsername()).isEqualTo("newuser");
            assertThat(result.getEmail()).isEqualTo("new@bookflow.com");
            assertThat(result.getFirstName()).isEqualTo("New");
            assertThat(result.getLastName()).isEqualTo("User");
            assertThat(result.getRole()).isEqualTo("USER");

            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode("Password1");
        }

        @Test
        @DisplayName("should throw when username is already taken")
        void shouldThrowWhenUsernameIsTaken() {
            when(userRepository.existsByUsername("newuser")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Username is already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when email is already in use")
        void shouldThrowWhenEmailIsInUse() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@bookflow.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerUser(signupRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Email is already in use");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when USER role is not found")
        void shouldThrowWhenRoleNotFound() {
            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@bookflow.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registerUser(signupRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Error: Role USER not found.");
        }
    }
}

