package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.config.JwtProperties;
import com.asms.springasms.dto.auth.CurrentUserResponse;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.security.JwtService;
import com.asms.springasms.service.AuthService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("OnnoRokomBackend", "OnnoRokomFrontend",
                "super-secret-jwt-signing-key-for-testing-at-least-32-chars", 60);
        mockMvc = mockMvc(new AuthController(authService, jwtService, props));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("System Admin");
        user.setEmail("admin@onnorokom.com");
        user.setRole(UserRole.Admin);
        user.setActive(true);
        user.setAuthVersion(1);
        return user;
    }

    @Test
    void login_success_shouldReturnTokenAndUser() throws Exception {
        User user = user();
        when(authService.authenticate(any())).thenReturn(Optional.of(user));
        when(jwtService.issueToken(user)).thenReturn("test-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@onnorokom.com\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.user.email").value("admin@onnorokom.com"))
                .andExpect(jsonPath("$.user.role").value("Admin"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void login_badCredentials_shouldReturn401Unauthorized() throws Exception {
        when(authService.authenticate(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@onnorokom.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void login_emptyBody_shouldReturn400ValidationShape() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("One or more validation errors occurred."))
                .andExpect(jsonPath("$.errors.Email").exists())
                .andExpect(jsonPath("$.errors.Password").exists());
    }

    @Test
    void me_authenticated_shouldReturnProfile() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.getCurrentUser(id)).thenReturn(Optional.of(
                new CurrentUserResponse(id, "System Admin", "admin@onnorokom.com", UserRole.Admin)));
        runAs(UserRole.Admin, id);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@onnorokom.com"));
    }

    @Test
    void logout_shouldReturn204() throws Exception {
        runAs(UserRole.Student);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
