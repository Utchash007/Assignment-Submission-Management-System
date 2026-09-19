package com.asms.springasms.security;

import com.asms.springasms.config.JwtProperties;
import com.asms.springasms.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String DEFAULT_SIGNING_KEY =
            "your-super-secret-signing-key-with-sufficient-length-2026";

    private final JwtProperties props;
    private final SecretKey secretKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        String rawKey = props.signingKey();
        if (rawKey == null || rawKey.isBlank() || rawKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            rawKey = DEFAULT_SIGNING_KEY;
        }
        this.secretKey = Keys.hmacShaKeyFor(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.accessTokenLifetimeMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
                .claim("nameid", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("auth_version", String.valueOf(user.getAuthVersion()))
                .issuer(props.issuer())
                .audience().add(props.audience()).and()
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(props.issuer())
                .requireAudience(props.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
