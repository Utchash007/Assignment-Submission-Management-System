package com.asms.springasms.controller;

import com.asms.springasms.config.JwtProperties;
import com.asms.springasms.dto.auth.AuthenticationResponse;
import com.asms.springasms.dto.auth.CurrentUserResponse;
import com.asms.springasms.dto.auth.LoginRequest;
import com.asms.springasms.entity.User;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.exception.UnauthorizedException;
import com.asms.springasms.security.CurrentUser;
import com.asms.springasms.security.JwtService;
import com.asms.springasms.service.AuthService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticate(request)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        String token = jwtService.issueToken(user);
        Instant expiresAt = Instant.now().plus(jwtProperties.accessTokenLifetimeMinutes(), ChronoUnit.MINUTES);
        CurrentUserResponse userDto = new CurrentUserResponse(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole());
        return ResponseEntity.ok(new AuthenticationResponse(token, expiresAt, userDto));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            throw new NotFoundException("User Not Found", "Authenticated user profile could not be found.");
        }
        return authService.getCurrentUser(currentUser.id())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("User Not Found",
                        "Authenticated user profile could not be found."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
