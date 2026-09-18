package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.user.ChangePasswordRequest;
import com.asms.springasms.dto.user.CreateUserRequest;
import com.asms.springasms.dto.user.SetActiveStatusRequest;
import com.asms.springasms.dto.user.UpdateUserRequest;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService() {
        return new UserService(userRepository, passwordEncoder);
    }

    private User existingUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Demo Student");
        user.setEmail("student@onnorokom.com");
        user.setRoll("S-1001");
        user.setPasswordHash(passwordEncoder.encode("Student@123"));
        user.setRole(UserRole.Student);
        user.setActive(true);
        user.setAuthVersion(1);
        return user;
    }

    @Test
    void createUser_duplicateEmail_shouldThrowWithRawEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@onnorokom.com")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService()
                .createUser(new CreateUserRequest("D", "  Dup@Onnorokom.com ", null, "secret1", UserRole.Student)));
        assertEquals("A user with email '  Dup@Onnorokom.com ' already exists.", ex.getMessage());
    }

    @Test
    void createUser_blankRoll_shouldStoreNullAndNormalize() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var created = userService().createUser(
                new CreateUserRequest(" New User ", "New@Onnorokom.com", "   ", "secret1", UserRole.Teacher));

        assertEquals("new@onnorokom.com", created.email());
        assertNull(created.roll());
        assertEquals("Teacher", created.role());
        assertTrue(created.isActive());
    }

    @Test
    void updateUser_sameEmailDifferentCase_shouldSkipDuplicateCheck() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var updated = userService().updateUser(user.getId(),
                new UpdateUserRequest("Demo Student", "STUDENT@onnorokom.com", ""));

        verify(userRepository, never()).existsByEmailIgnoreCaseAndIdNot(any(), any());
        assertNull(updated.roll());
    }

    @Test
    void updateUser_takenEmail_shouldThrow() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@onnorokom.com", user.getId()))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> userService().updateUser(user.getId(),
                new UpdateUserRequest("Demo Student", "taken@onnorokom.com", "S-1001")));
    }

    @Test
    void setActiveStatus_shouldAlwaysBumpAuthVersion() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService().setActiveStatus(user.getId(), new SetActiveStatusRequest(true));

        assertEquals(2, user.getAuthVersion());
    }

    @Test
    void changePassword_shouldRehashAndBumpAuthVersion() {
        User user = existingUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService().changePassword(user.getId(), new ChangePasswordRequest("BrandNew1"));

        assertTrue(passwordEncoder.matches("BrandNew1", user.getPasswordHash()));
        assertEquals(2, user.getAuthVersion());
    }

    @Test
    void getUserById_unknown_shouldThrowUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> userService().getUserById(id));
        assertEquals("User Not Found", ex.getTitle());
        assertTrue(ex.getMessage().contains(id.toString()));
    }
}
