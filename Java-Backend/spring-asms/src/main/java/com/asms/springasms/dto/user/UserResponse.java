package com.asms.springasms.dto.user;

import com.asms.springasms.entity.User;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String roll,
        String role,
        boolean isActive
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getRoll(), user.getRole().name(), user.isActive());
    }
}
