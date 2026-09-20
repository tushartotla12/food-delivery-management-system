package org.dmg.Dtos.Admin;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.dmg.Entities.Enums.Role;

public record CreateUserRequest(
        @NotBlank(message = "User's name is required")
        String name,
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Phone number required")
        String phone,
        @NotBlank(message = "Password is required")
        String password,
        @NotNull(message = "Role of user is required")
        Role role,
        Boolean active) {
}

