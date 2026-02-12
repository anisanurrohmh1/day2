package com.bankmega.authservice.controller;

import com.bankmega.authservice.dto.request.UpdateRolesRequest;
import com.bankmega.authservice.dto.response.ApiResponse;
import com.bankmega.authservice.dto.response.UserResponse;
import com.bankmega.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        log.info("Get current user request");
        UserResponse response = userService.getCurrentUser();
        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Get all users request");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LOAN_OFFICER', 'APPROVER')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Get user by ID request: {}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(
                ApiResponse.success("User retrieved successfully", response));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolesRequest request) {
        log.info("Update user roles request for user ID: {}", id);
        UserResponse response = userService.updateUserRoles(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("User roles updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long id) {
        log.info("Deactivate user request for user ID: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully", null));
    }
}
