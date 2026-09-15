# 09 — Seed Data

Source: `Seed/SeedData.cs` (199 lines) → Target: `seed/DataSeeder.java`.

## Behavior to Preserve

1. Runs at application startup, **only when the database is empty** (`if users exist → return`).
2. Seeds the exact demo dataset documented in the README (evaluators rely on these logins):

| Entity | Values |
|---|---|
| Users | `admin@onnorokom.com`/`Admin@123` (Admin), `teacher@onnorokom.com`/`Teacher@123` (Teacher), `student@onnorokom.com`/`Student@123` (Student, roll `S-1001`) |
| Term | `FALL2026` (2026-09-01 → 2026-12-31) |
| Batches | `BATCH-2026-A` "Batch 2026 Section A", `BATCH-2026-B` "Batch 2026 Section B" — both in FALL2026 |
| Courses | `CSE101` Intro to Programming, `CSE102` Data Structures & Algorithms, `CSE103` Database Management Systems |
| Batch enrollment | demo student → BATCH-2026-A (Active) |
| Course enrollments | student → CSE101 + CSE102 (Active) |
| Teacher allocations | demo teacher → CSE101 + CSE102 (Active) |
| Assignments | "Assignment 1: Variables and Control Flow" (CSE101, Published, 100 pts, +7 days, resubmission on); "Assignment 2: Linked List Reversal (Draft)" (CSE102, Draft, 50 pts, +14 days, resubmission off) |
| Submission | student → Assignment 1, answer text, `Submitted`, now |

3. Passwords hashed with **the same BCrypt** — `spring-security-crypto` hashes/verifies identically to `BCrypt.Net`.

## Translation Notes

- C# sets explicit `Guid Id`s; JPA `@GeneratedValue` supplies them — do not set IDs.
- C# sets FK Guids (`TermId = term.Id`); JPA sets the **entity reference** (`batch.setTerm(term)`), which is cleaner here since all objects are in hand.
- `DateOnly(2026, 9, 1)` → `LocalDate.of(2026, 9, 1)`; `DateTime.UtcNow.AddDays(7)` → `Instant.now().plus(7, ChronoUnit.DAYS)`.
- Enum literals are PascalCase per Option A (doc 02) — DB and JSON values unchanged.

## Full Port

```java
package com.onnorokom.backend.seed;

import com.onnorokom.backend.entity.*;
import com.onnorokom.backend.enums.*;
import com.onnorokom.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AcademicTermRepository termRepository;
    private final AcademicBatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final BatchEnrollmentRepository batchEnrollmentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final TeacherCourseAllocationRepository allocationRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {   // same guard as context.Users.AnyAsync()
            return;
        }

        // 1. Users
        User adminUser = new User();
        adminUser.setFullName("System Admin");
        adminUser.setEmail("admin@onnorokom.com");
        adminUser.setPasswordHash(passwordEncoder.encode("Admin@123"));
        adminUser.setRole(UserRole.Admin);
        adminUser.setActive(true);
        adminUser.setAuthVersion(1);

        User teacherUser = new User();
        teacherUser.setFullName("Demo Teacher");
        teacherUser.setEmail("teacher@onnorokom.com");
        teacherUser.setPasswordHash(passwordEncoder.encode("Teacher@123"));
        teacherUser.setRole(UserRole.Teacher);
        teacherUser.setActive(true);
        teacherUser.setAuthVersion(1);

        User studentUser = new User();
        studentUser.setFullName("Demo Student");
        studentUser.setEmail("student@onnorokom.com");
        studentUser.setRoll("S-1001");
        studentUser.setPasswordHash(passwordEncoder.encode("Student@123"));
        studentUser.setRole(UserRole.Student);
        studentUser.setActive(true);
        studentUser.setAuthVersion(1);

        userRepository.saveAll(java.util.List.of(adminUser, teacherUser, studentUser));

        // 2. Academic Term
        AcademicTerm term = new AcademicTerm();
        term.setCode("FALL2026");
        term.setStartsOn(LocalDate.of(2026, 9, 1));
        term.setEndsOn(LocalDate.of(2026, 12, 31));
        termRepository.save(term);

        // 3. Batches
        AcademicBatch batchA = new AcademicBatch();
        batchA.setTerm(term);
        batchA.setCode("BATCH-2026-A");
        batchA.setName("Batch 2026 Section A");

        AcademicBatch batchB = new AcademicBatch();
        batchB.setTerm(term);
        batchB.setCode("BATCH-2026-B");
        batchB.setName("Batch 2026 Section B");

        batchRepository.saveAll(java.util.List.of(batchA, batchB));

        // 4. Courses
        Course course1 = new Course();
        course1.setCode("CSE101");
        course1.setTitle("Introduction to Programming");
        course1.setDescription("Basics of programming with C# and .NET");

        Course course2 = new Course();
        course2.setCode("CSE102");
        course2.setTitle("Data Structures & Algorithms");
        course2.setDescription("Arrays, linked lists, trees, and algorithmic complexity");

        Course course3 = new Course();
        course3.setCode("CSE103");
        course3.setTitle("Database Management Systems");
        course3.setDescription("Relational schema design, SQL, and indexing");

        courseRepository.saveAll(java.util.List.of(course1, course2, course3));

        // 5. Batch Enrollment
        BatchEnrollment batchEnrollment = new BatchEnrollment();
        batchEnrollment.setBatch(batchA);
        batchEnrollment.setStudent(studentUser);
        batchEnrollment.setStatus(EnrollmentStatus.Active);
        batchEnrollmentRepository.save(batchEnrollment);

        // 6. Course Enrollments
        CourseEnrollment courseEnrollment1 = new CourseEnrollment();
        courseEnrollment1.setBatchEnrollment(batchEnrollment);
        courseEnrollment1.setCourse(course1);
        courseEnrollment1.setStatus(EnrollmentStatus.Active);

        CourseEnrollment courseEnrollment2 = new CourseEnrollment();
        courseEnrollment2.setBatchEnrollment(batchEnrollment);
        courseEnrollment2.setCourse(course2);
        courseEnrollment2.setStatus(EnrollmentStatus.Active);

        courseEnrollmentRepository.saveAll(java.util.List.of(courseEnrollment1, courseEnrollment2));

        // 7. Teacher Allocations
        TeacherCourseAllocation allocation1 = new TeacherCourseAllocation();
        allocation1.setTeacher(teacherUser);
        allocation1.setCourse(course1);
        allocation1.setStatus(TeacherCourseAllocationStatus.Active);

        TeacherCourseAllocation allocation2 = new TeacherCourseAllocation();
        allocation2.setTeacher(teacherUser);
        allocation2.setCourse(course2);
        allocation2.setStatus(TeacherCourseAllocationStatus.Active);

        allocationRepository.saveAll(java.util.List.of(allocation1, allocation2));

        // 8. Assignments
        Assignment assignment1 = new Assignment();
        assignment1.setCourse(course1);
        assignment1.setCreatedBy(teacherUser);
        assignment1.setTitle("Assignment 1: Variables and Control Flow");
        assignment1.setDescription("Implement basic control flow patterns and submit your solution.");
        assignment1.setDeadlineAt(Instant.now().plus(7, ChronoUnit.DAYS));
        assignment1.setMaximumMarks(new java.math.BigDecimal("100"));
        assignment1.setStatus(AssignmentStatus.Published);
        assignment1.setAllowResubmission(true);

        Assignment assignment2 = new Assignment();
        assignment2.setCourse(course2);
        assignment2.setCreatedBy(teacherUser);
        assignment2.setTitle("Assignment 2: Linked List Reversal (Draft)");
        assignment2.setDescription("Draft assignment for linked lists.");
        assignment2.setDeadlineAt(Instant.now().plus(14, ChronoUnit.DAYS));
        assignment2.setMaximumMarks(new java.math.BigDecimal("50"));
        assignment2.setStatus(AssignmentStatus.Draft);
        assignment2.setAllowResubmission(false);

        assignmentRepository.saveAll(java.util.List.of(assignment1, assignment2));

        // 9. Sample Submission
        Submission submission1 = new Submission();
        submission1.setAssignment(assignment1);
        submission1.setStudent(studentUser);
        submission1.setAnswerText("Here is my completed assignment for Variables and Control Flow.");
        submission1.setStatus(SubmissionStatus.Submitted);
        submission1.setSubmittedAt(Instant.now());
        submissionRepository.save(submission1);
    }
}
```

## Verification (fresh database only)

```bash
# drop/recreate the schema first (Hibernate validate-mode expects the EF schema —
# apply the EF-generated DDL or run the .NET backend once against the fresh DB, then):
mvn spring-boot:run
```

1. Startup log shows the seeder running (add an info log if you want parity visibility).
2. Log in as all three README accounts → 200 + token each.
3. Student sees 2 enrolled courses (CSE101, CSE102) and 1 published assignment.
4. Teacher sees 2 allocated courses, 1 published + 1 draft assignment, 1 submission on Assignment 1.
5. Second restart → seeder must be a no-op (`users` already populated).
```
