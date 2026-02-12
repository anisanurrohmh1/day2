package com.bankmega.authservice.service;

import com.bankmega.authservice.dto.request.UpdateRolesRequest;
import com.bankmega.authservice.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getCurrentUser();

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse updateUserRoles(Long id, UpdateRolesRequest request);

    void deactivateUser(Long id);
}
