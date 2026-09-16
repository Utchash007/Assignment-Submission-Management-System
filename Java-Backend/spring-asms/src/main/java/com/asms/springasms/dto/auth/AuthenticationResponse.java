package com.asms.springasms.dto.auth;

import java.time.Instant;

public record AuthenticationResponse(
        String accessToken,
        Instant expiresAt,
        CurrentUserResponse user
) {
}
