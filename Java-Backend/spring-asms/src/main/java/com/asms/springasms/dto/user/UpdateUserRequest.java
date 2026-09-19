package com.asms.springasms.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        String roll
) {
}
