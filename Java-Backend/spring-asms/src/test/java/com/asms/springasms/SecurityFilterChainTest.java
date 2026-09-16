package com.asms.springasms;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.config.JwtProperties;
import com.asms.springasms.config.SecurityConfig;
import com.asms.springasms.controller.AuthController;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.repository.UserRepository;
import com.asms.springasms.security.JwtAuthenticationFilter;
import com.asms.springasms.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitConfig
@WebAppConfiguration
class SecurityFilterChainTest {

    private static final String SECRET_KEY = "super-secret-jwt-signing-key-for-testing-at-least-32-chars";

    @RestController
    static class AdminTestController {
        @GetMapping("/api/admin-only")
        @PreAuthorize("hasRole('Admin')")
        public String adminOnly() {
            return "ok";
        }
    }

    @TestConfiguration
    @EnableWebMvc
    @Import({SecurityConfig.class, AuthController.class, AdminTestController.class})
    static class Config {
        @Bean
        public JwtProperties jwtProperties() {
            return new JwtProperties("OnnoRokomBackend", "OnnoRokomFrontend", SECRET_KEY, 60);
        }

        @Bean
        public JwtService jwtService(JwtProperties props) {
            return new JwtService(props);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
            return new JwtAuthenticationFilter(jwtService, userRepository);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void getMe_withoutToken_shouldReturn401EmptyBody() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void getMe_withBadToken_shouldReturn401EmptyBody() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.validtoken"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
    }

    @Test
    void getMe_withValidToken_shouldReturn200AndCurrentUserProfile() throws Exception {
        UUID userId = UUID.fromString("5c2c01b1-f00c-424d-ab09-d9e706cd19a6");
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@onnorokom.com");
        user.setFullName("System Admin");
        user.setRole(UserRole.Admin);
        user.setAuthVersion(1);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        String token = jwtService.issueToken(user);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("admin@onnorokom.com"))
                .andExpect(jsonPath("$.fullName").value("System Admin"))
                .andExpect(jsonPath("$.role").value("Admin"));
    }

    @Test
    void adminOnlyEndpoint_withStudentToken_shouldReturn403EmptyBody() throws Exception {
        UUID studentId = UUID.randomUUID();
        User student = new User();
        student.setId(studentId);
        student.setEmail("student@onnorokom.com");
        student.setFullName("Demo Student");
        student.setRole(UserRole.Student);
        student.setAuthVersion(1);
        student.setActive(true);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        String token = jwtService.issueToken(student);

        mockMvc.perform(get("/api/admin-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));
    }

    @Test
    void adminOnlyEndpoint_withAdminToken_shouldReturn200() throws Exception {
        UUID adminId = UUID.randomUUID();
        User admin = new User();
        admin.setId(adminId);
        admin.setEmail("admin@onnorokom.com");
        admin.setFullName("System Admin");
        admin.setRole(UserRole.Admin);
        admin.setAuthVersion(1);
        admin.setActive(true);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        String token = jwtService.issueToken(admin);

        mockMvc.perform(get("/api/admin-only").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void logout_shouldReturn204EmptyBody() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@onnorokom.com");
        user.setRole(UserRole.Admin);
        user.setAuthVersion(1);
        user.setActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        String token = jwtService.issueToken(user);

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
