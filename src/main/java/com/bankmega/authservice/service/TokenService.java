package com.bankmega.authservice.service;

import com.bankmega.authservice.dto.response.AuthResponse;
import com.bankmega.authservice.entity.User;

public interface TokenService {

    AuthResponse generateAuthResponse(User user);

    AuthResponse refreshAccessToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);

    void cleanupExpiredTokens();
}
