package com.asms.springasms.service;

import com.asms.springasms.dto.auth.CurrentUserResponse;
import com.asms.springasms.dto.auth.LoginRequest;
import com.asms.springasms.entity.User;
import com.asms.springasms.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<User> authenticate(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        return userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(User::isActive)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()));
    }

    @Transactional(readOnly = true)
    public Optional<CurrentUserResponse> getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .map(user -> new CurrentUserResponse(
                        user.getId(), user.getFullName(), user.getEmail(), user.getRole()));
    }
}
