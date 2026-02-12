package com.bankmega.authservice.service;

import com.bankmega.authservice.dto.request.LoginRequest;
import com.bankmega.authservice.dto.request.RefreshTokenRequest;
import com.bankmega.authservice.dto.request.RegisterRequest;
import com.bankmega.authservice.dto.response.AuthResponse;
import com.bankmega.authservice.dto.response.TokenResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);
}
