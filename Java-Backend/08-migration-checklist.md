# 08 — Migration Checklist (Execution Plan)

Ordered, verifiable steps. Do not start a phase before the previous phase's verification passes.

## Phase 0 — Contract Snapshot (before writing Java)

- [ ] Run the .NET backend with seed data.
- [ ] Capture the API surface: `curl` every endpoint family (login, users, terms, batches, courses, enrollments, allocations, assignments, submissions, attachments) and save request/response JSON pairs to `Java-Migrate/contract-snapshots/`.
- [ ] Note exact URL casing from `Frontend/src/lib/api/*.ts` (Spring paths are case-sensitive).
- [ ] Decode one login JWT at jwt.io — record claim names/values.

## Phase 1 — Skeleton (doc 01)

- [ ] Generate Maven project, all dependencies, `application.yml` pointing at the existing database.
- [ ] `BackendApplication` + one health endpoint.
- [ ] `ddl-auto: validate` — boots clean against existing schema (no entities yet = trivially passes).
- [ ] Swagger UI reachable at `/swagger-ui.html`.

## Phase 2 — Entities & Enums (doc 02)

- [ ] 5 enums (decide naming Option A — keep PascalCase `Admin`/`Active` for DB + JSON parity).
- [ ] 10 entities with annotations per the checklist table in doc 02.
- [ ] Soft-delete fields modeled (`deletedAt` on Assignment).
- [ ] Boot with `ddl-auto: validate` — **must pass with zero schema complaints**.
- [ ] Write a `@DataJpaTest` or main-runner smoke check: load one user, one assignment.

## Phase 3 — Repositories (doc 03)

- [ ] 10 `JpaRepository` interfaces.
- [ ] Soft-delete-predicated methods for Assignment/Submission/Attachment aggregates (`findActive...`).
- [ ] Derived queries for the known service needs (fill in as services port).

## Phase 4 — Error & Security Plumbing (docs 06, 07)

- [ ] `NotFoundException`, `ForbiddenException`, `GlobalExceptionHandler`.
- [ ] `JwtProperties`, `JwtService`, `JwtAuthenticationFilter`, `CurrentUser`, `SecurityConfig`.
- [ ] `BCryptPasswordEncoder` bean.
- [ ] Verify: **.NET-issued token works on Spring backend** (`GET /api/auth/me`).
- [ ] Verify: problem+json shape for 401/403/404/400 matches Phase 0 snapshots.

## Phase 5 — Services + Controllers, domain by domain (docs 04, 05)

Port in dependency order — service then its controller, verify, next:

- [ ] **Auth**: `AuthService` + `AuthController` (login/me/logout) — token claims must match Phase 0 decode.
- [ ] **Users**: CRUD, status toggle, password reset (AuthVersion++).
- [ ] **AcademicTerms**: CRUD + date-range validation.
- [ ] **Batches**: CRUD + student assignment + roster.
- [ ] **Courses**: CRUD.
- [ ] **CourseEnrollments**: bulk batch→course, status toggle, student course queries.
- [ ] **TeacherCourseAllocations**: allocate + status toggle + teacher's courses.
- [ ] **Assignments**: CRUD, publish, close-submissions, soft delete; teacher-allocation guard.
- [ ] **Submissions**: upsert (one per student), late detection, resubmission gate, review/grading.
- [ ] **SubmissionAttachments**: upload (MIME + size), list, download, delete.

Per-domain verification ritual:

```
1. mvn compile                         # clean build
2. run backend against existing DB     # (no reseed — data is already there)
3. replay Phase 0 curl snapshots       # diff status + JSON body
4. exercise RBAC with all 3 seeded roles
```

## Phase 6 — Seeder & Startup (doc 01)

- [ ] `DataSeeder` (ApplicationRunner) ported — full translation in [09-seed-data.md](09-seed-data.md); only runs when `userRepository.count() == 0`.
- [ ] Verify on a **fresh database**: seed matches README's documented accounts and sample data.

## Phase 7 — OpenAPI & Frontend Swap

- [ ] springdoc covers all 50 endpoints; JWT security scheme configured for Swagger "Authorize" button.
- [ ] Point `Frontend/.env.local` / docker-compose at the Spring backend.
- [ ] Full manual pass: login → dashboard → course → assignment → submit → grade → feedback, per role (admin/teacher/student).

## Phase 8 — Infra & Cutover

- [ ] New `Dockerfile` (Maven multi-stage, doc 01), docker-compose updated.
- [ ] Render: new service or in-place swap; env vars renamed (`DB_CONN` → datasource vars, `Jwt__SigningKey` → `JWT_SIGNING_KEY`).
- [ ] Smoke test in staging; only then retire `Backend/OnnorokomBackend`.

## Risk Register (where bugs will come from)

| Risk | Mitigation |
|---|---|
| Route casing mismatches (`api/AcademicTerms` vs `api/academicterms`) | Phase 0 snapshot + frontend `lib/api` strings copied verbatim |
| Enum JSON values (`Admin` vs `ADMIN`) | Keep PascalCase enum names (Option A) |
| Soft-delete rows leaking through `findById` | Only `findActive...` repo methods on those aggregates; grep-review |
| JWT claim name drift | Cross-validate tokens between backends in Phase 4 |
| EAGER fetch N+1 / lazy `LazyInitializationException` | `open-in-view: false` + LAZY everywhere + DTO mapping inside transaction |
| ProblemDetails JSON shape drift | Parity curl diffs in Phases 4 & 5 |
| `bytea` 10MB uploads exhausting Tomcat memory | Keep multipart limits; monitor; out of scope to change storage |

## Out of Scope (explicitly not migrating)

- EF Core `Migrations/` — Hibernate runs `validate` only; schema changes go manual SQL going forward.
- `I*Controller`/`I*Service` abstraction interfaces.
- Storage architecture change (bytea → object storage) — possible follow-up, not part of parity migration.
- Frontend — untouched by design.
