# 02 — Entities & Enums

Source: `Models/Entities/*.cs` + `DbContext/Configurations/*.cs` → Target: `entity/` + `enums/` packages.

EF Core's fluent configuration classes (`AcademicBatchConfiguration.cs` etc.) all collapse into **JPA annotations on the entities**. There is no separate configuration layer.

## Enums (5 files, trivial ports)

```java
public enum UserRole { ADMIN, TEACHER, STUDENT }
public enum EnrollmentStatus { ACTIVE, INACTIVE }
public enum TeacherCourseAllocationStatus { ACTIVE, INACTIVE }
public enum AssignmentStatus { DRAFT, PUBLISHED }
public enum SubmissionStatus { SUBMITTED, LATE, REVIEWED, RETURNED }
```

**Critical**: .NET enum values are `Admin`, `Student` (PascalCase) stored as strings in the DB. Java convention is `ADMIN`. Since we keep the existing database, decide **one** of:

- **Option A (recommended)**: Keep Java names `Admin`, `Teacher`... → DB strings match, zero data migration. Slightly un-idiomatic Java.
- **Option B**: Use `ADMIN` + `@JsonValue`/`@JsonCreator` mapping and run a one-time `UPDATE` SQL to convert stored strings.

The API contract (JSON) must keep returning `"Admin"`, `"Student"` etc., because the frontend types (`Frontend/src/types/enums.ts`) expect them. With Option A this is automatic.

## Entity Template

Every EF configuration rule has exactly one JPA annotation equivalent:

| EF Core (`Configurations/*.cs`) | JPA annotation |
|---|---|
| `ToTable("academic_terms")` | `@Table(name = "academic_terms")` |
| `HasKey(x => x.Id)` | `@Id @GeneratedValue` on field |
| `HasMaxLength(200).IsRequired()` | `@Column(nullable = false, length = 200)` |
| `HasColumnType("text")` | `@Column(columnDefinition = "text")` |
| `HasColumnType("timestamp with time zone")` | `@Column(columnDefinition = "timestamptz")` — or just map `Instant` (Npgsql-style) |
| `HasPrecision(8, 2)` | `@Column(precision = 8, scale = 2)` |
| `HasConversion<string>()` | `@Enumerated(EnumType.STRING)` |
| `HasIndex(...).IsUnique()` | `@Table(uniqueConstraints = @UniqueConstraint(...))` or `@UniqueIndex` via `@Index` |
| `HasQueryFilter(DeletedAt == null)` | **no equivalent** — see Soft Delete section |
| `OnDelete(DeleteBehavior.Restrict)` | `@ManyToOne` + **don't** cascade; also add `@OnDelete(action = OnDeleteAction.RESTRICT)` from Hibernate annotations to match DDL (only matters if Hibernate manages DDL; with `validate` it's documentation) |

## Worked Example: `User`

```csharp
// C# today: User.cs + UserConfiguration.cs (2 files)
```

```java
// Java: entity/User.java (1 file, everything included)
package com.onnorokom.backend.entity;

import com.onnorokom.backend.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(name = "ix_users_email", columnNames = "email"))
@Getter @Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 100)
    private String roll;

    @Column(name = "password_hash", nullable = false, length = 500)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role = UserRole.Admin;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "auth_version", nullable = false)
    private int authVersion = 1;

    // ---- navigations (one-directional by default, see below) ----

    @OneToMany(mappedBy = "student")
    private List<BatchEnrollment> batchEnrollments = new ArrayList<>();

    @OneToMany(mappedBy = "teacher")
    private List<TeacherCourseAllocation> teacherCourseAllocations = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy")
    private List<Assignment> createdAssignments = new ArrayList<>();

    @OneToMany(mappedBy = "student")
    private List<Submission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "evaluatedBy")
    private List<Submission> evaluatedSubmissions = new ArrayList<>();
}
```

Lombok note: **do NOT use `@Data` on entities** — generated `equals`/`hashCode`/`toString` touch lazy collections and cause proxy issues. Use `@Getter @Setter` only. Put `@Data` on DTOs.

## Relationship Mapping Rules

EF Core's bidirectional navigations + FK shadow properties map like this:

```java
// AcademicBatch.cs: TermId FK + Term nav + Enrollments collection
@Entity
@Table(name = "academic_batches",
       uniqueConstraints = @UniqueConstraint(name = "uq_batches_term_code",
                                             columnNames = {"term_id", "code"}))
public class AcademicBatch {

    @Id @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;              // FK column comes from @JoinColumn

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @OneToMany(mappedBy = "batch")
    private List<BatchEnrollment> enrollments = new ArrayList<>();
}
```

Rules that keep you out of trouble:

1. **Always `FetchType.LAZY` on `@ManyToOne`** (JPA defaults to EAGER → N+1 queries and accidental loads). `@OneToMany` is LAZY by default.
2. **Prefer one-directional navigation.** EF Core needs both sides; JPA doesn't. Keep the child→parent side (`Submission.assignment`); only add parent→child collections when the code actually traverses them. This kills the classic `toString`/`equals` recursion bugs.
3. **Don't expose FK `Guid TermId` properties** like EF does. The entity reference *is* the FK (`term.getId()` after load, `em.getReference(AcademicTerm.class, id)` to set without loading).
4. Cascade types: **none** (matches `ON DELETE RESTRICT` everywhere in the .NET configs).

## The Soft-Delete Problem (hardest part of the migration)

EF Core global query filters:

- `assignments`: `DeletedAt == null`
- `submissions`: `Assignment.DeletedAt == null`
- `submission_attachments`: `Submission.Assignment.DeletedAt == null`

Spring Data JPA has no automatic global filter. **Strategy: Hibernate 6.4+ `@SoftDelete` + repository discipline.**

```java
@Entity
@org.hibernate.annotations.SoftDelete(columnName = "deleted_at")  // maps Instant? — verify type support; fallback below
public class Assignment { ... }
```

Because `@SoftDelete` support for timestamp columns and cross-entity cascading filters is limited, the **pragmatic plan** is:

1. `Assignment` keeps its `deletedAt` field as a normal `Instant` column.
2. Every `AssignmentRepository` query method includes `AND a.deletedAt IS NULL` explicitly (naming convention `findAllActive...` or `@Query`).
3. `SubmissionRepository` / `SubmissionAttachmentRepository` methods join through assignment and add `AND a.deleted_at IS NULL`.
4. Optional safety net: Hibernate `@FilterDef(name = "notDeleted", ...)` + `@Filter` enabled in a small request interceptor — only add if queries keep being forgotten.

Document in `08-migration-checklist.md`: grep for all repository methods touching these three aggregates and add the predicate. This is where migration bugs will live.

## Full Entity Checklist

| Entity | Table | FKs | Unique indexes | Special |
|---|---|---|---|---|
| User | `users` | — | email | authVersion, isActive |
| AcademicTerm | `academic_terms` | — | code | `LocalDate` × 2 |
| AcademicBatch | `academic_batches` | term | (term_id, code) | — |
| BatchEnrollment | `batch_enrollments` | batch, student | (student_id, batch_id) | enum status |
| Course | `courses` | — | code | text description |
| CourseEnrollment | `course_enrollments` | batch_enrollment, course | (batch_enrollment_id, course_id) | enum status |
| TeacherCourseAllocation | `teacher_course_allocations` | teacher, course | (teacher_id, course_id) | enum status |
| Assignment | `assignments` | course, created_by | — | **soft delete**, timestamptz, decimal(8,2) |
| Submission | `submissions` | assignment, student, evaluated_by? | (assignment_id, student_id) | **soft-delete cascade filter** |
| SubmissionAttachment | `submission_attachments` | submission | — | **soft-delete cascade filter**, `bytea` |

## Verification

```bash
# after writing all entities:
mvn spring-boot:run   # with ddl-auto=validate against the existing DB
```

If Hibernate starts without schema errors, entity mappings match the EF Core-generated schema. Any mismatch is reported at boot with the exact table/column — fix before proceeding.
