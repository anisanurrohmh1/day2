package com.bankmega.authservice.controller;

import com.bankmega.authservice.dto.request.LoginRequest;
import com.bankmega.authservice.dto.request.RefreshTokenRequest;
import com.bankmega.authservice.dto.request.RegisterRequest;
import com.bankmega.authservice.dto.response.ApiResponse;
import com.bankmega.authservice.dto.response.AuthResponse;
import com.bankmega.authservice.dto.response.TokenResponse;
import com.bankmega.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username: {}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("Login request for username: {}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request received");
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("Logout request received");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(
                ApiResponse.success("Logout successful", null));
    }
}
