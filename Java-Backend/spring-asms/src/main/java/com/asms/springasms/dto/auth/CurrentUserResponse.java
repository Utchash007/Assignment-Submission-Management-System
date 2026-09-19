package com.asms.springasms.dto.auth;

import com.asms.springasms.enums.UserRole;
import java.util.UUID;

public record CurrentUserResponse(
        UUID id,
        String fullName,
        String email,
        UserRole role
) {
}
