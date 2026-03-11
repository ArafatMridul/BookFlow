package com.example.bookflowproject.controller;

import com.example.bookflowproject.dto.LoginRequest;
import com.example.bookflowproject.dto.LoginResponse;
import com.example.bookflowproject.dto.SignupRequest;
import com.example.bookflowproject.dto.UserDTO;
import com.example.bookflowproject.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API endpoints for JWT-based authentication.
 * Used by API clients (e.g., frontend AJAX calls for book/user/borrowing operations).
 *
 * POST /api/auth/login   → returns JWT token
 * POST /api/auth/signup  → registers user and returns UserDTO
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDTO> signup(@Valid @RequestBody SignupRequest signupRequest) {
        UserDTO user = authService.registerUser(signupRequest);
        return ResponseEntity.ok(user);
    }
}

