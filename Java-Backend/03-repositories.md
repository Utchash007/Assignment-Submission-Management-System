# 03 — Repositories (and the Death of Unit of Work)

Source: `Repository/IRepository.cs`, `Repository/EFRepository.cs`, `UnitOfWork/IUnitOfWork.cs`, `UnitOfWork/UnitOfWork.cs` → Target: `repository/` package with Spring Data JPA interfaces.

## Do Not Port These

The .NET backend has three layers of persistence abstraction:

```
IRepository<TEntity>  (contract: Get/GetAll/Add/Update/Delete)
EFRepository<TEntity> (EF Core implementation)
IUnitOfWork           (property-bag of 10 repos + SaveChangesAsync)
```

**Spring Data JPA replaces all three.** A repository is now a one-line interface:

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
}
```

`JpaRepository` already provides `findById`, `findAll`, `save`, `delete`, `count`, `existsById`, paging, etc. — the exact surface of `IRepository<>` plus more.

## Translation Rules

| .NET pattern | Spring Data equivalent |
|---|---|
| `unitOfWork.UserRepo.Get(id)` | `userRepository.findById(id)` → `Optional<User>` |
| `unitOfWork.UserRepo.GetAll()` (then LINQ) | derived queries (`findBy...`) or `@Query` JPQL |
| `unitOfWork.UserRepo.Add(entity)` | `userRepository.save(entity)` |
| `unitOfWork.UserRepo.Update(entity)` | `userRepository.save(entity)` (same call — merge semantics) |
| `unitOfWork.UserRepo.Delete(entity)` | `userRepository.delete(entity)` |
| `unitOfWork.SaveChangesAsync()` | **nothing** — `@Transactional` commits on method exit |

### LINQ → Query Methods

The services compose LINQ over `GetAll()`. Each such composition becomes one repository method:

```csharp
// AuthService.cs — what the C# does
unitOfWork.UserRepo.GetAll()
    .SingleOrDefaultAsync(u => u.Email.ToLower() == normalizedEmail);
```

```java
// UserRepository.java — derived query (method name IS the query)
Optional<User> findByEmailIgnoreCase(String email);
```

Complex compositions use JPQL:

```csharp
// typical service query: enrolled courses for a student in active batch enrollments
unitOfWork.CourseEnrollmentRepo.GetAll()
    .Where(ce => ce.Status == EnrollmentStatus.Active
              && ce.BatchEnrollment.StudentId == studentId)
    .Select(ce => ce.Course)
```

```java
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {

    @Query("""
           select ce.course
           from CourseEnrollment ce
           where ce.status = com.onnorokom.backend.enums.EnrollmentStatus.Active
             and ce.batchEnrollment.student.id = :studentId
           """)
    List<Course> findActiveCoursesByStudentId(@Param("studentId") UUID studentId);
}
```

Notes:
- Named parameters (`:studentId`) are positional-string based — always use `@Param`.
- Enums in JPQL need the fully qualified name or an enum literal parameter.
- Projecting DTOs directly (`select new com.onnorokom...dto.CourseResponse(...)`) is allowed but projections in the service layer keep parity with the C# mapping code — translate, don't redesign.

## Transactions Replace Unit of Work

The .NET code calls `unitOfWork.SaveChangesAsync()` at the end of each service method — one implicit transaction per save.

Spring equivalent — annotate the service method:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        // validation, password hashing ...
        User user = new User();
        // map fields
        userRepository.save(user);      // participates in transaction
        return UserResponse.from(user); // still in transaction, lazy loads OK
        // commit happens here — on exception, rollback is automatic
    }
}
```

Rules:

1. `@Transactional` goes on **service methods** (or the service class), never on controllers or repositories (repos already have it for single operations).
2. **Read-only queries**: `@Transactional(readOnly = true)` for find/list methods — enables Hibernate flush-mode optimizations.
3. Spring's `RuntimeException` rollback matches EF behavior (EF rolls back nothing unless `SaveChanges` throws).
4. Keep transactions **short** — file bytea uploads (10MB) inside a long transaction hold DB connections; the .NET code already separates upload from submission, preserve that separation.

## Repository Inventory (what to create)

| Interface | Key methods beyond `JpaRepository` |
|---|---|
| `UserRepository` | `findByEmailIgnoreCase`, `existsByEmailIgnoreCaseAndIdNot`, `findByRole`, `findByBatchId` (for rosters) |
| `AcademicTermRepository` | `findByCode`, `existsByCode` |
| `AcademicBatchRepository` | `findByTermId`, `findByTermCode` |
| `BatchEnrollmentRepository` | `findByBatchId`, `findByStudentId`, `findByStudentIdAndBatchId`, `findActiveByBatchId` |
| `CourseRepository` | `findByCode`, `existsByCode` |
| `CourseEnrollmentRepository` | `findByCourseId`, `findByBatchEnrollmentIds`, `findActiveCoursesByStudentId`, `existsByCourseIdAndStudentId` |
| `TeacherCourseAllocationRepository` | `findByCourseId`, `findByTeacherId`, `findByTeacherIdAndStatus`, `existsByTeacherIdAndCourseId` |
| `AssignmentRepository` | `findByCourseIdAndStatus`, `findActiveById` (soft-delete predicate!), `findByStudentCourses...` |
| `SubmissionRepository` | `findByAssignmentId`, `findByAssignmentIdAndStudentId`, `existsByAssignmentIdAndStudentId` |
| `SubmissionAttachmentRepository` | `findBySubmissionId`, `findByIdAndSubmissionId` |

Each of these corresponds to a LINQ expression that exists today in `Services/*`. While porting a service, write the repository method it needs — don't pre-invent methods.

## Soft-Delete Discipline (repeat from doc 02)

Every repository touching `Assignment`, `Submission`, or `SubmissionAttachment` must filter soft-deleted rows:

```java
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("select a from Assignment a where a.id = :id and a.deletedAt is null")
    Optional<Assignment> findActiveById(@Param("id") UUID id);

    @Query("""
           select a from Assignment a
           where a.course.id = :courseId
             and a.status = com.onnorokom.backend.enums.AssignmentStatus.Published
             and a.deletedAt is null
           """)
    List<Assignment> findPublishedByCourseId(@Param("courseId") UUID courseId);
}
```

`findById` (inherited) must **not** be used for these aggregates in service code — always the `findActive...` variants. Add a code-review checklist item for this.

## `bytea` Streaming Caution

`SubmissionAttachment.fileData` is `byte[]` mapped as `bytea` — 10MB byte arrays in heap. The .NET code does the same, so parity is fine, but note for later optimization: PostgreSQL large objects (`@Lob` + OID) or filesystem/S3 storage would reduce heap pressure. **Not in scope for migration.**
