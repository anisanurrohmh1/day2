package com.bankmega.authservice.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankmega.authservice.config.JwtConfig;
import com.bankmega.authservice.dto.response.AuthResponse;
import com.bankmega.authservice.dto.response.UserResponse;
import com.bankmega.authservice.entity.RefreshToken;
import com.bankmega.authservice.entity.User;
import com.bankmega.authservice.exception.TokenException;
import com.bankmega.authservice.repository.RefreshTokenRepository;
import com.bankmega.authservice.repository.UserRepository;
import com.bankmega.authservice.security.CustomUserDetailsService;
import com.bankmega.authservice.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final CustomUserDetailsService userDetailsService;

    @Override
    @Transactional
    public AuthResponse generateAuthResponse(User user) {
        log.info("Generating authentication response for user: {}", user.getUsername());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);

        String refreshTokenValue = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(jwtConfig.getRefreshTokenExpirationMs() / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(expiryDate)
                .build();

        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toSet()))
                .isActive(user.getIsActive())
                .build();

        log.info("Successfully generated authentication response for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpirationMs() / 1000)
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        log.info("Refreshing access token");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token provided");
                    return new TokenException("Invalid refresh token");
                });

        if (token.isExpired()) {
            log.warn("Refresh token has expired");
            refreshTokenRepository.delete(token);
            throw new TokenException("Refresh token has expired");
        }

        if (token.getIsRevoked()) {
            log.warn("Refresh token has been revoked");
            throw new TokenException("Refresh token has been revoked");
        }

        User user = token.getUser();
        if (!user.getIsActive()) {
            log.warn("User account is inactive: {}", user.getUsername());
            throw new TokenException("User account is inactive");
        }

        log.info("Successfully refreshed access token for user: {}", user.getUsername());
        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        log.info("Revoking refresh token");

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Invalid refresh token provided for revocation");
                    return new TokenException("Invalid refresh token");
                });

        token.setIsRevoked(true);
        refreshTokenRepository.save(token);

        log.info("Successfully revoked refresh token for user: {}", token.getUser().getUsername());
    }

    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");

        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.deleteExpiredTokens(now);

        log.info("Cleanup completed. Deleted expired refresh tokens");
    }
}
