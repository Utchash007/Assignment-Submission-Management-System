package com.asms.springasms.controller;

import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.GlobalExceptionHandler;
import com.asms.springasms.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Shared standalone-MockMvc setup for controller tests (services are mocked).
 * Authentication is installed directly in the {@code SecurityContextHolder} —
 * deterministic under standalone setup, where request post-processors that rely
 * on the security filter chain do not apply.
 */
final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static MockMvc mockMvc(Object... controllers) {
        return MockMvcBuilders.standaloneSetup(controllers)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(new MockEnvironment()))
                .build();
    }

    static void runAs(UserRole role, UUID id) {
        CurrentUser principal = new CurrentUser(id, "Test User", "test@onnorokom.com", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    static void runAs(UserRole role) {
        runAs(role, UUID.randomUUID());
    }

    static void clearAuth() {
        SecurityContextHolder.clearContext();
    }
}
