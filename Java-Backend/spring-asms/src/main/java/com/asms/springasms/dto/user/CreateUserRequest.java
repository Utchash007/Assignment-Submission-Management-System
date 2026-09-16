package com.asms.springasms.dto.user;

import com.asms.springasms.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String roll,
        @NotBlank @Size(min = 6) String password,
        @NotNull UserRole role
) {
}
