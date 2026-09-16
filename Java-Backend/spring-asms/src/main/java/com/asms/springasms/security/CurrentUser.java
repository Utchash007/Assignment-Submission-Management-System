package com.asms.springasms.security;

import com.asms.springasms.enums.UserRole;
import java.util.UUID;

public record CurrentUser(
        UUID id,
        String fullName,
        String email,
        UserRole role
) {
}
