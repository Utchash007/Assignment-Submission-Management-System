# 06 — Security & Auth

Source: `Program.cs` (JWT wiring, `OnTokenValidated`), `Controllers/Auth/AuthController.cs` (token issuance), `Helpers/*` → Target: `config/SecurityConfig`, `security/JwtAuthenticationFilter`, `security/JwtService`, `security/CurrentUser`.

## What Must Be Preserved

1. JWT: HS256, same issuer/audience/signing key env vars → **tokens issued by the .NET backend remain valid** (and vice versa).
2. Claims: `nameid` (user id), `email`, `role`, `auth_version` — exact claim names, because the filter reads them.
3. Per-request DB check: user exists + `IsActive` + `AuthVersion == token.auth_version` (the .NET `OnTokenValidated` hook).
4. RBAC matrix from `[Authorize(Roles = ...)]` on each endpoint.
5. 401/403 as ProblemDetails JSON, not Spring's default headers-only response.

## Claim Name Mapping — Subtle and Important

.NET's `ClaimTypes.NameIdentifier` serializes to JWT claim **`nameid`**, and `ClaimTypes.Role` → **`role`** (short names after JWT mapping). The custom `auth_version` claim is already a plain string. In JJWT you control names directly:

```java
// security/JwtService.java — token issuance (replaces AuthController's JwtSecurityTokenHandler code)
@Service
public class JwtService {

    private final JwtProperties props;                     // from app.jwt.* in application.yml
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.signingKey().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(User user) {
        Instant expiresAt = Instant.now().plus(props.accessTokenLifetimeMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .claim("nameid", user.getId().toString())      // must match .NET's ClaimTypes.NameIdentifier
                .claim("email", user.getEmail())
                .claim("role",  user.getRole().name())          // .NET ClaimTypes.Role → "role"
                .claim("auth_version", String.valueOf(user.getAuthVersion()))
                .issuer(props.issuer())
                .audience().add(props.audience()).and()
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
```

> Verify against a real token from the running .NET backend (`jwt.io`): the claim set must match exactly, or cross-issued tokens break. If .NET emits `nameid` vs `name_id` depending on mapping behavior, adjust to whatever the current backend emits — decode one live login token to confirm before writing the filter.

## The Auth Filter (replaces `OnTokenValidated`)

```java
// security/JwtAuthenticationFilter.java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);   // anonymous; SecurityFilterChain decides
            return;
        }

        try {
            Claims claims = jwtService.parse(header.substring(7));   // validates sig, exp, iss, aud

            UUID userId = UUID.fromString(claims.get("nameid", String.class));
            int tokenAuthVersion = Integer.parseInt(claims.get("auth_version", String.class));

            // the per-request DB validation .NET does in OnTokenValidated
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive() || user.getAuthVersion() != tokenAuthVersion) {
                writeProblem(response, 401, "The token is no longer valid.");
                return;
            }

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            var principal = new CurrentUser(user.getId(), user.getEmail(), user.getRole()); // our record

            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            writeProblem(response, 401, "Invalid or expired token.");
            return;
        }

        chain.doFilter(request, response);
    }
}
```

`CurrentUser` (record) replaces `Helpers/ClaimsPrincipalExtensions.GetUserId()`:

```java
public record CurrentUser(UUID id, String email, UserRole role) { }

// usage in any controller:
CurrentUser me = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
```

Or better, a `@AuthenticationPrincipal CurrentUser me` controller parameter — Spring injects it automatically.

## SecurityConfig (replaces `Program.cs` auth wiring)

```java
// config/SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(CsrfConfigurer::disable)                    // stateless JWT API (CSRF irrelevant)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()     // [AllowAnonymous]
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**",
                                       "/swagger-ui.html", "/").permitAll() // docs (replaces Scalar)
                        .anyRequest().authenticated())                      // [Authorize] default
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::unauthorized)       // 401 ProblemDetails
                        .accessDeniedHandler(this::forbidden))              // 403 ProblemDetails
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();    // verifies BCrypt.Net hashes as-is
    }
}
```

Because `anyRequest().authenticated()` is the default, plain `[Authorize]` endpoints need no annotation. Role restrictions move to `@PreAuthorize` per endpoint (doc 05).

## RBAC Matrix (extract from the C# controllers)

Port these `Roles = "..."` attributes into `@PreAuthorize` — build the table while porting each controller, this is the starter:

| Area | Admin | Teacher | Student | Public |
|---|---|---|---|---|
| `POST /api/auth/login` | ✓ | ✓ | ✓ | **yes** |
| Users CRUD / reset password / status | **only** | — | — | — |
| Academic terms CRUD | **only** | read? | — | — |
| Batches CRUD + student assign | **only** | read? | — | — |
| Courses CRUD | **only** | read | read | — |
| Enrollments (batch→course) | **only** | read | read (own) | — |
| Teacher allocations | **only** | read (own) | read | — |
| Assignment create/edit/publish/close/delete | **only** | **own allocated courses** | read published | — |
| Submission upsert | — | — | **own** | — |
| Submission review/grade | — | **own courses** | — | — |
| Attachment upload/download | — | download (own courses) | own | — |

Several rules are **resource-based, not just role-based** (e.g., "teacher may edit assignments only for courses allocated to them"). .NET enforces those inside services. Keep them in services — `@PreAuthorize` handles only the coarse role gate.

## CORS

`Program.cs` allows any origin/header/method:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    var config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
// + .cors(c -> c.configurationSource(corsConfigurationSource())) in the filter chain
```

(The frontend proxies through Next.js rewrites anyway — CORS mostly matters for Swagger testing, same as today.)

## Logout Endpoint

`POST /api/auth/logout` returns 204 with no server work (JWTs are stateless) — same as the .NET version. One line.

## Verification Plan

1. Log in against Spring backend with seeded `admin@onnorokom.com` — BCrypt hash from seeder must verify.
2. Decode the issued token at jwt.io — claims `{nameid, email, role, auth_version}` match a token from the .NET backend.
3. Call `GET /api/auth/me` with the token — 200 with user payload.
4. Reset password via API → old token → next request must be 401 (`AuthVersion` mismatch).
5. Deactivate user → token → 401.
6. Role checks: student token against admin endpoint → 403 ProblemDetails.
