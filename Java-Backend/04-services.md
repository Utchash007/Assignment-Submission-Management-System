# 04 — Services

Source: `Services/*/[I]XService.cs` (10 pairs) → Target: `service/` package, plain `@Service` classes.

## Translation Rules

| .NET | Spring |
|---|---|
| `class AuthService(IAuthService)` + interface | `@Service class AuthService` — **drop the interface** unless multiple impls needed |
| constructor injection via primary ctor | `@RequiredArgsConstructor` + `final` fields |
| `unitOfWork.XRepo` | injected `XRepository` field |
| `async Task<T>` / `await` | plain return `T` |
| `CancellationToken ct` | delete the parameter |
| `X is null` checks | `Optional<T>` — `orElseThrow`, `map`, `ifPresent` |
| service-thrown exceptions for errors | keep the same exception types (doc 07) |

## Worked Example: `AuthService`

The full C# service (Auth/AuthService.cs) translates to:

```java
package com.onnorokom.backend.service;

import com.onnorokom.backend.dto.auth.*;
import com.onnorokom.backend.entity.User;
import com.onnorokom.backend.enums.UserRole;
import com.onnorokom.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

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
                        user.getId(), user.getFullName(), user.getEmail(), user.getRole().name()));
    }
}
```

Key points:

1. **`Optional` instead of `null`** — the .NET code returns `User?` and controllers check `is null`. Preserve semantics with `Optional` and let the controller branch (`orElse(null)` if you want literal parity — but idiomatic Java prefers Optional end-to-end).
2. **BCrypt parity**: `spring-security-crypto`'s `BCryptPasswordEncoder.matches()` verifies BCrypt.Net hashes unchanged (same `$2a$` format). **Seeded users can log in to the Spring backend on day one.** Hashing new passwords: `passwordEncoder.encode(raw)`.
3. Note the C# `SingleOrDefaultAsync(u => u.Email.ToLower() == normalizedEmail)` becomes the derived query `findByEmailIgnoreCase` — the translation happened in the repository (doc 03).

## Business-Rule Ports (verbatim logic)

All deadline / RBAC / grading rules move as plain Java. Example — submission deadline check (found in `SubmissionService`):

```csharp
// C# shape
if (assignment.Status != AssignmentStatus.Published
    || assignment.SubmissionsClosedAt is not null
    || DateTime.UtcNow > assignment.DeadlineAt)
{
    throw new InvalidOperationException("Submissions are closed for this assignment.");
}
```

```java
// Java shape — same rules, same exception semantics
if (assignment.getStatus() != AssignmentStatus.Published
        || assignment.getSubmissionsClosedAt() != null
        || Instant.now().isAfter(assignment.getDeadlineAt())) {
    throw new IllegalStateException("Submissions are closed for this assignment.");
}
```

Exception mapping (full table in doc 07):
- `InvalidOperationException` → `IllegalStateException` → HTTP 400
- `KeyNotFoundException` → custom `NotFoundException` (or `NoSuchElementException`) → HTTP 404
- `UnauthorizedAccessException` → custom `ForbiddenException` → HTTP 403

## DTO Mapping

The C# services construct response DTOs with object initializers. Java DTOs should be **records** (immutable, auto `equals`/`hashCode`/`toString`, Jackson-friendly):

```java
// dto/user/UserResponse.java
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String roll,
        String role,
        boolean isActive
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getRoll(), user.getRole().name(), user.isActive());
    }
}
```

Rules:
- One `from(Entity)` static factory per response DTO — the single place where entity→DTO mapping happens (same role as the C# inline initializers).
- Records serialize camelCase by default — matches the API contract.
- Enums serialize as `.name()` automatically; with Option A naming (doc 02) the wire format is unchanged (`"Admin"`, `"Published"`, ...).

## Service-by-Service Port Notes

| Service | C# highlights to preserve |
|---|---|
| `AuthService` | email normalize + BCrypt + IsActive filter (shown above) |
| `UserService` | create/update/reset-password (increments `AuthVersion`), activate/deactivate, roll assignment, role-specific validation |
| `AcademicTermService` | start/end date validation (start < end) |
| `BatchService` | unique (term, code), student assignment to batch, enrollment status toggling |
| `CourseService` | unique course code, catalog CRUD |
| `CourseEnrollmentService` | **bulk batch→course enrollment** — multi-row insert in one `@Transactional` method; roster queries with student active filter |
| `TeacherCourseAllocationService` | unique (teacher, course), status toggling, "allocated courses for teacher" query |
| `AssignmentService` | draft/publish lifecycle, deadline defaulting, **soft delete** (`DeletedAt = now`), close-submissions (`SubmissionsClosedAt = now`), teacher-must-be-allocated check |
| `SubmissionService` | one-per-student-per-assignment upsert, late detection (`SubmittedAt > DeadlineAt` → `Late`), resubmission gate (`AllowResubmission` + deadline), review with marks bounds `0..MaximumMarks` + feedback required |
| `SubmissionAttachmentService` | MIME allowlist + 10MB limit (uses `FileUploadOptions` → `FileUploadProperties`), upload separate from submission answer |

**Port order**: Auth → Users → Terms → Batches → Courses → Enrollments → Allocations → Assignments → Submissions → Attachments (dependency order; each only needs the repositories already written).

## AuthVersion Increment (password reset flow)

```java
@Transactional
public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found."));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
        throw new IllegalStateException("Current password is incorrect.");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setAuthVersion(user.getAuthVersion() + 1);   // invalidates issued JWTs
    // save is implicit — dirty checking flushes on commit
}
```

Note there is **no explicit `save()`** needed: `@Transactional` + managed entity = dirty checking flushes changes. Calling `save()` explicitly is fine too and closer to the C# style.
