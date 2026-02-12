package com.bankmega.authservice.service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankmega.authservice.dto.request.LoginRequest;
import com.bankmega.authservice.dto.request.RefreshTokenRequest;
import com.bankmega.authservice.dto.request.RegisterRequest;
import com.bankmega.authservice.dto.response.AuthResponse;
import com.bankmega.authservice.dto.response.TokenResponse;
import com.bankmega.authservice.entity.Role;
import com.bankmega.authservice.entity.RoleName;
import com.bankmega.authservice.entity.User;
import com.bankmega.authservice.exception.DuplicateResourceException;
import com.bankmega.authservice.exception.ResourceNotFoundException;
import com.bankmega.authservice.repository.RoleRepository;
import com.bankmega.authservice.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AuditService auditService;



    @Value("${app.username.superadmin}")
    private String superAdminUsername;
    @Value("${app.password.superadmin}")
    private String superAdminPassword;

    @Transactional(rollbackFor = Exception.class)
    @PostConstruct  
    public void initSuperAdmin() {
        Optional<User> currentUser = userRepository.findByUsername(superAdminUsername);
        if (currentUser.isPresent()) return;

        Role superAdminRole = roleRepository.findByRoleName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(null,RoleName.ADMIN, "")));
        Role customerRole = roleRepository.findByRoleName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(null,RoleName.CUSTOMER, "")));
        Role agentRole = roleRepository.findByRoleName(RoleName.APPROVER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(null,RoleName.APPROVER, "")));
        Role officerRole = roleRepository.findByRoleName(RoleName.LOAN_OFFICER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(null,RoleName.LOAN_OFFICER, "")));

        User user = User.builder()
                .username(superAdminUsername)
                .email("email@mail.com")
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .fullName("Super Admin")
                .roles(Set.of(superAdminRole, agentRole, officerRole))
                .isActive(true)
                .build();

        userRepository.save(user);
    }


    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username already exists - {}", request.getUsername());
            throw new DuplicateResourceException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists - {}", request.getEmail());
            throw new DuplicateResourceException("Email already exists");
        }

        Role customerRole = roleRepository.findByRoleName(RoleName.CUSTOMER)
                .orElseThrow(() -> {
                    log.error("CUSTOMER role not found in database");
                    return new ResourceNotFoundException("Default role not found");
                });

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .roles(Collections.singleton(customerRole))
                .isActive(true)
                .build();

        userRepository.save(user);

        log.info("Successfully registered user: {}", user.getUsername());

        auditService.logAction("REGISTER", "User registered: " + user.getUsername());

        return tokenService.generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.error("User not found after successful authentication: {}", request.getUsername());
                        return new ResourceNotFoundException("User not found");
                    });

            if (!user.getIsActive()) {
                log.warn("Login failed: user account is inactive - {}", request.getUsername());
                throw new ResourceNotFoundException("User account is inactive");
            }

            log.info("Successfully authenticated user: {}", user.getUsername());

            auditService.logAction("LOGIN", "User logged in: " + user.getUsername());

            return tokenService.generateAuthResponse(user);

        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {} - {}", request.getUsername(), e.getMessage());
            auditService.logAction("LOGIN_FAILED", "Failed login attempt: " + request.getUsername());
            throw e;
        }
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refreshing token");

        AuthResponse authResponse = tokenService.refreshAccessToken(request.getRefreshToken());

        auditService.logAction("TOKEN_REFRESH", "Access token refreshed");

        return TokenResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .tokenType(authResponse.getTokenType())
                .expiresIn(authResponse.getExpiresIn())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        log.info("User logout");

        tokenService.revokeRefreshToken(refreshToken);

        auditService.logAction("LOGOUT", "User logged out");

        log.info("Successfully logged out user");
    }
}
