package com.bankmega.authservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRolesRequest {

    @NotEmpty(message = "At least one role is required")
    private Set<String> roles;
}
