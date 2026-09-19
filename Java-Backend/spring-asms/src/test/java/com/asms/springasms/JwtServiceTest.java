package com.asms.springasms;

import static org.junit.jupiter.api.Assertions.*;

import com.asms.springasms.config.JwtProperties;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET_KEY = "super-secret-jwt-signing-key-for-testing-at-least-32-chars";
    private JwtProperties props;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        props = new JwtProperties("OnnoRokomBackend", "OnnoRokomFrontend", SECRET_KEY, 60);
        jwtService = new JwtService(props);
    }

    @Test
    void issueToken_shouldProduceExpectedClaimsMatchingDotNetContract() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("admin@onnorokom.com");
        user.setFullName("System Admin");
        user.setRole(UserRole.Admin);
        user.setAuthVersion(1);
        user.setActive(true);

        String token = jwtService.issueToken(user);
        assertNotNull(token);

        Claims claims = jwtService.parseClaims(token);
        assertEquals(userId.toString(), claims.get("nameid", String.class));
        assertEquals("admin@onnorokom.com", claims.get("email", String.class));
        assertEquals("Admin", claims.get("role", String.class));
        assertEquals("1", claims.get("auth_version", String.class));
        assertEquals("OnnoRokomBackend", claims.getIssuer());
        assertTrue(claims.getAudience().contains("OnnoRokomFrontend"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void parseClaims_shouldSuccessfullyAcceptTokenIssuedByDotNetFormat() {
        UUID userId = UUID.fromString("5c2c01b1-f00c-424d-ab09-d9e706cd19a6");
        Instant now = Instant.now();
        Instant exp = now.plus(60, ChronoUnit.MINUTES);

        // Mimic ASP.NET Core JwtSecurityTokenHandler output
        String dotNetToken = Jwts.builder()
                .claim("nameid", userId.toString())
                .claim("email", "admin@onnorokom.com")
                .claim("role", "Admin")
                .claim("auth_version", "1")
                .issuer("OnnoRokomBackend")
                .audience().add("OnnoRokomFrontend").and()
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        Claims claims = jwtService.parseClaims(dotNetToken);
        assertEquals("5c2c01b1-f00c-424d-ab09-d9e706cd19a6", claims.get("nameid", String.class));
        assertEquals("admin@onnorokom.com", claims.get("email", String.class));
        assertEquals("Admin", claims.get("role", String.class));
        assertEquals("1", claims.get("auth_version", String.class));
    }

    @Test
    void parseClaims_shouldRejectExpiredToken() {
        UUID userId = UUID.randomUUID();
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant expPast = past.plus(1, ChronoUnit.HOURS);

        String expiredToken = Jwts.builder()
                .claim("nameid", userId.toString())
                .claim("email", "admin@onnorokom.com")
                .claim("role", "Admin")
                .claim("auth_version", "1")
                .issuer("OnnoRokomBackend")
                .audience().add("OnnoRokomFrontend").and()
                .issuedAt(Date.from(past))
                .expiration(Date.from(expPast))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThrows(JwtException.class, () -> jwtService.parseClaims(expiredToken));
    }

    @Test
    void parseClaims_shouldRejectTamperedToken() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@onnorokom.com");
        user.setRole(UserRole.Admin);
        user.setAuthVersion(1);
        user.setActive(true);

        String token = jwtService.issueToken(user);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThrows(JwtException.class, () -> jwtService.parseClaims(tamperedToken));
    }
}
