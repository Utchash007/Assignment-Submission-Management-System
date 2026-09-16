package com.asms.springasms.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "The Email field is required.")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                message = "The Email field is not a valid e-mail address.")
        String email,
        @NotBlank(message = "The Password field is required.")
        String password
) {
}
