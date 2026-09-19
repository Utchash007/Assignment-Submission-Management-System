# ASP.NET Core → Spring Boot Migration Guidelines

This folder contains the complete migration playbook for porting `Backend/OnnorokomBackend` (.NET 10 / EF Core) to Spring Boot 3 (Java 21 / Spring Data JPA).

## Target Stack Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Build tool | **Maven** | Single module, predictable, matches Spring docs |
| Web layer | **Spring MVC** (`spring-boot-starter-web`) | REST/JSON controllers — not views |
| Persistence | **Spring Data JPA + Hibernate** | Replaces EF Core + Npgsql |
| Database | **PostgreSQL 16** (unchanged) | Zero schema changes; reuse existing DB |
| Security | **Spring Security + JJWT** | JWT Bearer + RBAC parity |
| Boilerplate | **Lombok** | C#-like brevity for entities/DTOs |
| Docs | **springdoc-openapi** (Swagger UI) | Replaces Scalar |
| Validation | **Bean Validation (`spring-boot-starter-validation`)** | Replaces Zod-equivalent DTO attributes |
| Architecture | **Layered (Controller → Service → Repository)** — same shape as the .NET backend | Minimal rewrite risk |

## Document Index

| # | Document | Covers |
|---|---|---|
| 1 | [01-project-setup.md](01-project-setup.md) | Maven project, dependencies, package structure, `application.yml`, Docker changes |
| 2 | [02-entities-and-enums.md](02-entities-and-enums.md) | 10 entities → JPA annotations, soft-delete strategy, date/decimal mapping |
| 3 | [03-repositories.md](03-repositories.md) | Killing `IRepository`/`IUnitOfWork`, Spring Data JPA repositories, transactions |
| 4 | [04-services.md](04-services.md) | Service translation rules, LINQ → JPA criteria/JPQL, async model |
| 5 | [05-controllers.md](05-controllers.md) | Routing, status codes, DTO serialization, response conventions |
| 6 | [06-security-and-auth.md](06-security-and-auth.md) | JWT issuance/validation, `AuthVersion` invalidation filter, RBAC matrix |
| 7 | [07-exceptions-and-validation.md](07-exceptions-and-validation.md) | RFC 7807 ProblemDetails via `@RestControllerAdvice`, Bean Validation |
| 8 | [08-migration-checklist.md](08-migration-checklist.md) | Ordered execution plan with verification steps |
| 9 | [09-seed-data.md](09-seed-data.md) | Full `DataSeeder.java` port of `SeedData.cs` with parity verification |

## Golden Rules

1. **Do not change the API contract.** The Next.js frontend is untouched. Same routes (`/api/...`), same camelCase JSON, same string enums, same ProblemDetails error shape, same status codes.
2. **Do not change the database.** Reuse the existing PostgreSQL schema; run Hibernate with `ddl-auto=validate`. EF migrations are retired once parity is reached.
3. **Translate, don't redesign.** Every C# service has a 1:1 Java counterpart. Resist the urge to refactor business logic during migration.
4. **`IUnitOfWork` does not survive.** It is an EF Core pattern; Spring Data JPA + `@Transactional` is the idiomatic replacement (see doc 3).
5. **Verify each layer independently** — build, run, hit endpoints with curl against the same seed data before moving to the next layer.
