package com.asms.springasms.controller;

import com.asms.springasms.dto.auth.CurrentUserResponse;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        if (currentUser == null) {
            throw new NotFoundException("User Not Found", "Authenticated user profile could not be found.");
        }
        return ResponseEntity.ok(new CurrentUserResponse(
                currentUser.id(),
                currentUser.fullName(),
                currentUser.email(),
                currentUser.role()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
