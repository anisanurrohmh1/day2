package com.bankmega.authservice.service;

import com.bankmega.authservice.dto.request.UpdateRolesRequest;
import com.bankmega.authservice.dto.response.UserResponse;
import com.bankmega.authservice.entity.Role;
import com.bankmega.authservice.entity.RoleName;
import com.bankmega.authservice.entity.User;
import com.bankmega.authservice.exception.ResourceNotFoundException;
import com.bankmega.authservice.repository.RoleRepository;
import com.bankmega.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        log.debug("Getting current authenticated user");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new ResourceNotFoundException("No authenticated user found");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", username);
                    return new ResourceNotFoundException("User not found");
                });

        log.debug("Retrieved current user: {}", username);

        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Retrieving all users");

        List<User> users = userRepository.findAll();

        log.info("Retrieved {} users", users.size());

        return users.stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Retrieving user by ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });

        log.info("Retrieved user: {}", user.getUsername());

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long id, UpdateRolesRequest request) {
        log.info("Updating roles for user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });

        // Convert role names to Role entities
        Set<Role> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            try {
                RoleName roleEnum = RoleName.valueOf(roleName);
                Role role = roleRepository.findByRoleName(roleEnum)
                        .orElseThrow(() -> {
                            log.warn("Role not found: {}", roleName);
                            return new ResourceNotFoundException("Role not found: " + roleName);
                        });
                roles.add(role);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role name: {}", roleName);
                throw new ResourceNotFoundException("Invalid role name: " + roleName);
            }
        }

        // Update user roles
        user.setRoles(roles);
        userRepository.save(user);

        log.info("Successfully updated roles for user: {}", user.getUsername());

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        log.info("Deactivating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });

        user.setIsActive(false);
        userRepository.save(user);

        log.info("Successfully deactivated user: {}", user.getUsername());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toSet()))
                .isActive(user.getIsActive())
                .build();
    }
}
