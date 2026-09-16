package com.asms.springasms.security;

import com.asms.springasms.entity.User;
import com.asms.springasms.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        try {
            Claims claims = jwtService.parseClaims(token);
            String nameId = claims.get("nameid", String.class);
            String authVersionStr = claims.get("auth_version", String.class);

            if (nameId != null && authVersionStr != null) {
                UUID userId = UUID.fromString(nameId);
                int tokenAuthVersion = Integer.parseInt(authVersionStr);

                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.isActive() && user.getAuthVersion() == tokenAuthVersion) {
                    CurrentUser principal = new CurrentUser(
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            user.getRole()
                    );

                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT authentication failed: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
