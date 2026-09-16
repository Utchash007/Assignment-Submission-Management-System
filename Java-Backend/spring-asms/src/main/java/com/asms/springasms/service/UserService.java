package com.asms.springasms.service;

import com.asms.springasms.dto.user.ChangePasswordRequest;
import com.asms.springasms.dto.user.CreateUserRequest;
import com.asms.springasms.dto.user.SetActiveStatusRequest;
import com.asms.springasms.dto.user.UpdateUserRequest;
import com.asms.springasms.dto.user.UserResponse;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(UserRole roleFilter) {
        List<User> users = roleFilter == null
                ? userRepository.findAllByOrderByFullNameAsc()
                : userRepository.findByRoleOrderByFullNameAsc(roleFilter);
        return users.stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User Not Found",
                        "User with ID '" + id + "' was not found."));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException(
                    "A user with email '" + request.email() + "' already exists.");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setRoll(request.roll() == null || request.roll().isBlank() ? null : request.roll().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        user.setAuthVersion(1);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found",
                        "User with ID '" + id + "' was not found."));
        String normalizedEmail = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)) {
            throw new IllegalStateException(
                    "A user with email '" + request.email() + "' already exists.");
        }
        user.setEmail(normalizedEmail);
        user.setFullName(request.fullName().trim());
        user.setRoll(request.roll() == null || request.roll().isBlank() ? null : request.roll().trim());
        return UserResponse.from(user);
    }

    @Transactional
    public void setActiveStatus(UUID id, SetActiveStatusRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found",
                        "User with ID '" + id + "' was not found."));
        user.setActive(request.isActive());
        user.setAuthVersion(user.getAuthVersion() + 1);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User Not Found",
                        "User with ID '" + id + "' was not found."));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setAuthVersion(user.getAuthVersion() + 1);
    }
}
