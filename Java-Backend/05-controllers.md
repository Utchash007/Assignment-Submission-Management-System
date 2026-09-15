# 05 — Controllers

Source: `Controllers/**/*Controller.cs` (10 controllers, ~50 endpoints) → Target: `controller/` package, `@RestController` classes.

## Anatomy Translation

```csharp
// C# today — AuthController.cs
[ApiController]
[Route("api/[controller]")]
public class AuthController(IAuthService authService, IOptions<JwtOptions> jwtOptions)
    : ControllerBase, IAuthController
{
    [HttpPost("login")]
    [AllowAnonymous]
    public async Task<IActionResult> Login([FromBody] LoginRequest request, CancellationToken ct)
    { ... return Ok(response); }
}
```

```java
// Java — AuthController.java
package com.onnorokom.backend.controller;

import com.onnorokom.backend.dto.auth.*;
import com.onnorokom.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;   // token building lives here, see doc 06

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticate(request)
                .map(user -> ResponseEntity.ok(jwtService.buildLoginResponse(user)))
                .orElseGet(() -> ResponseEntity.status(401).build()); // see error convention below
    }
}
```

## Attribute Map

| ASP.NET Core | Spring MVC |
|---|---|
| `[ApiController]` | (nothing — `@RestController` implies it) |
| `[Route("api/[controller]")]` | `@RequestMapping("/api/auth")` — **no auto-name convention, write paths explicitly** |
| `[HttpGet]` `[HttpPost]` `[HttpPut]` `[HttpPatch]` `[HttpDelete]` | `@GetMapping` `@PostMapping` `@PutMapping` `@PatchMapping` `@DeleteMapping` |
| `[HttpPost("login")]` | `@PostMapping("/login")` |
| `[Authorize]` | (secured by default — see SecurityConfig; no annotation needed) |
| `[AllowAnonymous]` | `@ PermitAll` not needed — instead whitelist the path in `SecurityFilterChain` |
| `[Authorize(Roles = "Admin")]` | `@PreAuthorize("hasRole('ADMIN')")` (needs `@EnableMethodSecurity`) |
| `[Authorize(Roles = "Admin,Teacher")]` | `@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")` |
| `[FromBody]` | `@RequestBody` |
| `[FromRoute]` | `@PathVariable` |
| `[FromQuery]` | `@RequestParam` |
| `[FromForm]` | `@RequestParam` (multipart) or `@RequestPart` |
| `CreatedAtAction(...)` (201 + Location) | `ResponseEntity.created(URI.create(...)).body(dto)` |
| `NoContent()` | `ResponseEntity.noContent().build()` |
| `IActionResult` / `ActionResult<T>` | `ResponseEntity<T>` or bare DTO (bare = always 200) |

## URL Case Convention — Watch Out

.NET's `[controller]` token produces the **C# class name** (`UsersController` → `/api/Users` — wait, no: `[controller]` strips "Controller" and keeps the casing → `/api/Users`). **Actually .NET keeps the class name casing minus suffix: `api/Users`.** Check the actual casing your frontend calls (e.g. `lib/api/users.ts` builds the URLs) and pin Spring paths to match:

- `@RequestMapping("/api/users")` — Spring MVC paths are case-sensitive by default.
- The frontend calls e.g. `api/Users/...`? Read `Frontend/src/lib/api/*.ts` and mirror every path exactly. **Do this before writing any controller** — it's the source of truth for the contract.

## Status Code Convention

The .NET controllers return `Ok(dto)` (200), `Created(...)` (201), `NoContent()` (204), and ProblemDetails for errors. Spring equivalent:

```java
@PostMapping
public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
    CourseResponse created = courseService.create(request);
    return ResponseEntity
            .created(URI.create("/api/courses/" + created.id()))  // 201 + Location
            .body(created);
}

@DeleteMapping("/{id}")
public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    userService.deactivate(id);
    return ResponseEntity.noContent().build();                     // 204
}
```

Errors (401/403/404/400) are **thrown from services as exceptions** and converted to ProblemDetails by the global handler (doc 07) — never built manually in controllers. This matches the .NET middleware behavior and keeps controllers thin.

## DTO + Validation

Requests are records with Bean Validation annotations (replaces .NET's data attributes on DTOs):

```java
// dto/course/CreateCourseRequest.java
public record CreateCourseRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description
) {}
```

`@Valid` on the parameter triggers validation → 400 ProblemDetails with field errors (doc 07 shows the handler).

## Multipart File Upload (SubmissionAttachments)

The attachment endpoints accept `IFormFile` in .NET:

```java
@PostMapping("/api/submissions/{submissionId}/attachments")
public ResponseEntity<AttachmentResponse> upload(
        @PathVariable UUID submissionId,
        @RequestParam("file") MultipartFile file) {

    AttachmentResponse response = attachmentService.upload(submissionId, file);
    return ResponseEntity.status(201).body(response);
}
```

Service-side validation (MIME allowlist, ≤10MB) ports from `SubmissionAttachmentService` + `FileUploadOptions`. Spring's `spring.servlet.multipart.max-file-size` (doc 01) is the outer guard.

## Controller Inventory & Route Map

Fill this table from the frontend `lib/api/` modules while porting — it becomes the acceptance checklist:

| Controller | Base path | Endpoints (verify against C#) | RBAC |
|---|---|---|---|
| Auth | `/api/auth` | `POST /login`, `POST /logout`, `GET /me` | login=public, rest=any |
| Users | `/api/users` | CRUD + `PATCH /{id}/status`, `POST /{id}/reset-password`, `GET /{id}` | Admin (read: Teacher?) |
| AcademicTerms | `/api/academicterms`? | CRUD | Admin |
| Batches | `/api/batches` | CRUD + `POST /{id}/students`, roster | Admin |
| Courses | `/api/courses` | CRUD + enrollments + allocations sub-routes | mixed |
| CourseEnrollments | `/api/courseenrollments` | bulk enroll, status toggle | Admin |
| TeacherCourseAllocations | `/api/teachercourseallocations` | allocate, status toggle | Admin |
| Assignments | `/api/assignments` | CRUD, `PATCH /{id}/publish`, `PATCH /{id}/close-submissions`, soft `DELETE` | Teacher+Admin |
| Submissions | `/api/submissions` | upsert, `GET /by-assignment/{id}`, `PATCH /{id}/review` | mixed |
| SubmissionAttachments | `/api/submissionattachments` | upload, list, download, delete | mixed |

**Note on `[controller]` casing**: .NET's `AcademicTermsController` → route `api/AcademicTerms`. The frontend files (`academic-terms.ts` etc.) contain the exact strings — copy them verbatim into `@RequestMapping`. Route mismatches are the #1 migration bug class; the checklist in doc 08 includes a route-parity test.

## What Gets Deleted

- The `I*Controller.cs` interfaces (10 files) — no purpose in Spring.
- `User.GetUserId()` helper → `CurrentUser.id()` from the security package (doc 06).
- `ProblemDetails` manual construction in controllers → global handler.
