# 07 — Exceptions & Validation

Source: `Middleware/GlobalExceptionMiddleware.cs`, DTO validation attributes → Target: `exception/GlobalExceptionHandler.java` (`@RestControllerAdvice`), Bean Validation on request records.

## Goal

Identical RFC 7807 `application/problem+json` responses, so the frontend `lib/api/client.ts` error handling (which reads `title`/`detail`) works unchanged.

## The .NET Behavior Being Replicated

```csharp
// GlobalExceptionMiddleware.cs — the exception → (status, title, detail) mapping
KeyNotFoundException        → 404 "Resource Not Found"
UnauthorizedAccessException → 403 "Forbidden"
InvalidOperationException  → 400 "Bad Request"
ArgumentException           → 400 "Invalid Argument"
_                            → 500 "Internal Server Error" (detail only in Development)
```

Plus framework errors: model binding/validation → 400, `[Authorize]` failures → 401/403 (handled in SecurityConfig, doc 06).

## Custom Exceptions

```java
// exception/ package
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
```

Java's built-ins cover the rest:
- `IllegalStateException` ≈ `InvalidOperationException` → 400
- `IllegalArgumentException` ≈ `ArgumentException` → 400
- Validation failures → `MethodArgumentNotValidException` → 400 with field errors

## GlobalExceptionHandler

```java
package com.onnorokom.backend.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // KeyNotFoundException
    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    // UnauthorizedAccessException
    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail forbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    // InvalidOperationException
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail badOperation(IllegalStateException ex) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // ArgumentException (includes jakarta ConstraintViolationException)
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ProblemDetail badArgument(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage());
    }

    // 10MB upload limit (FileUploadOptions parity)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail uploadTooLarge(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.BAD_REQUEST, "Upload Too Large", "File exceeds the 10MB limit.");
    }

    // unknown path → 404 instead of Spring's default
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail noRoute(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", "The requested endpoint does not exist.");
    }

    // fallback: 500, detail hidden in prod
    @ExceptionHandler(Exception.class)
    ProblemDetail generic(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.");
        // log the real exception here (SLF4J); if you want dev-mode detail like
        // the C# env.IsDevelopment() branch, inject Environment and branch on acceptsProfiles
    }

    // @Valid request body failures → 400 + field details
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more validation errors occurred.");
        pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> java.util.Map.of("field", fe.getField(), "message", fe.getDefaultMessage()))
                .toList());
        return pd;
    }

    private ProblemDetail build(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        return pd;   // Spring serializes as application/problem+json automatically
    }
}
```

Spring 6's `ProblemDetail` emits RFC 7807 with `type`, `title`, `status`, `detail` — JSON shape matches the .NET middleware output (both add `instance`; verify with curl and align via `pd.setProperty`/`@JsonProperty` if the frontend depends on a field Spring omits).

## Service Exception Translation Table

While porting services, replace throw statements mechanically:

| C# throw | Java throw | HTTP |
|---|---|---|
| `new KeyNotFoundException("Course not found.")` | `new NotFoundException("Course not found.")` | 404 |
| `new UnauthorizedAccessException("...")` | `new ForbiddenException("...")` | 403 |
| `new InvalidOperationException("...")` | `new IllegalStateException("...")` | 400 |
| `new ArgumentException("...")` | `new IllegalArgumentException("...")` | 400 |

## Bean Validation (replaces DTO data annotations)

The .NET DTOs use validation attributes. Java records get the same via `jakarta.validation`:

| .NET | Bean Validation |
|---|---|
| `[Required]` | `@NotNull` / `@NotBlank` (strings) |
| `[EmailAddress]` | `@Email` |
| `[StringLength(200)]` | `@Size(max = 200)` |
| `[Range(0, 100)]` | `@DecimalMin("0") @DecimalMax("100")` |
| `[Min]` / `[Max]` | `@Min` / `@Max` |

Example — the review request (marks between 0 and assignment max is enforced in the service; static bounds on the DTO):

```java
public record ReviewSubmissionRequest(
        @NotNull @DecimalMin("0") @DecimalMax("10000") BigDecimal marks,
        @NotNull ReviewAction action,          // enum: Reviewed | Returned
        @Size(max = 5000) String feedback
) {}
```

Activation for controller params: `@Valid @RequestBody` (body) or `@Validated` on the class for `@PathVariable`/`@RequestParam` constraints.

## Error Response Parity Checklist

Run these against **both** backends (old .NET, new Spring) and diff the JSON:

```
GET  /api/auth/me                 (no token)          → 401 problem+json
GET  /api/auth/me                 (bad token)         → 401 problem+json
GET  /api/users/{random-guid}     (admin token)       → 404 problem+json
POST /api/auth/login              (invalid body)      → 400 problem+json
POST /api/auth/login              (wrong password)    → 401 problem+json
GET  /api/users                   (student token)     → 403 problem+json
POST /api/assignments             (teacher, invalid)  → 400 problem+json w/ field errors
```

Any shape difference (`detail` vs `errors`, casing) must be normalized — `lib/api/client.ts` parses these fields literally.
