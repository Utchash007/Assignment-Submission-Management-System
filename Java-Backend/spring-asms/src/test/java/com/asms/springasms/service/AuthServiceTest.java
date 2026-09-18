package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.auth.LoginRequest;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService() {
        return new AuthService(userRepository, passwordEncoder);
    }

    private User user(boolean active, String rawPassword) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("System Admin");
        user.setEmail("admin@onnorokom.com");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.Admin);
        user.setActive(active);
        user.setAuthVersion(1);
        return user;
    }

    @Test
    void authenticate_validCredentials_shouldReturnUser() {
        User user = user(true, "Admin@123");
        when(userRepository.findByEmailIgnoreCase("admin@onnorokom.com"))
                .thenReturn(Optional.of(user));

        assertEquals(Optional.of(user),
                authService().authenticate(new LoginRequest("  Admin@Onnorokom.com ", "Admin@123")));
    }

    @Test
    void authenticate_wrongPassword_shouldBeEmpty() {
        when(userRepository.findByEmailIgnoreCase("admin@onnorokom.com"))
                .thenReturn(Optional.of(user(true, "Admin@123")));

        assertTrue(authService()
                .authenticate(new LoginRequest("admin@onnorokom.com", "wrong")).isEmpty());
    }

    @Test
    void authenticate_inactiveUser_shouldBeEmpty() {
        when(userRepository.findByEmailIgnoreCase("admin@onnorokom.com"))
                .thenReturn(Optional.of(user(false, "Admin@123")));

        assertTrue(authService()
                .authenticate(new LoginRequest("admin@onnorokom.com", "Admin@123")).isEmpty());
    }

    @Test
    void authenticate_unknownEmail_shouldBeEmpty() {
        when(userRepository.findByEmailIgnoreCase("nobody@onnorokom.com"))
                .thenReturn(Optional.empty());

        assertTrue(authService()
                .authenticate(new LoginRequest("nobody@onnorokom.com", "Admin@123")).isEmpty());
    }

    @Test
    void getCurrentUser_activeUser_shouldReturnProfile() {
        User user = user(true, "Admin@123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var profile = authService().getCurrentUser(user.getId()).orElseThrow();
        assertEquals(user.getId(), profile.id());
        assertEquals("System Admin", profile.fullName());
        assertEquals(UserRole.Admin, profile.role());
    }

    @Test
    void getCurrentUser_inactiveUser_shouldBeEmpty() {
        User user = user(false, "Admin@123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertTrue(authService().getCurrentUser(user.getId()).isEmpty());
    }
}
