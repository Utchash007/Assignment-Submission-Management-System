package com.asms.springasms.seed;

import com.asms.springasms.entity.AcademicBatch;
import com.asms.springasms.entity.AcademicTerm;
import com.asms.springasms.entity.Assignment;
import com.asms.springasms.entity.BatchEnrollment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.CourseEnrollment;
import com.asms.springasms.entity.Submission;
import com.asms.springasms.entity.TeacherCourseAllocation;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.AssignmentStatus;
import com.asms.springasms.enums.EnrollmentStatus;
import com.asms.springasms.enums.SubmissionStatus;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.repository.AcademicBatchRepository;
import com.asms.springasms.repository.AcademicTermRepository;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.BatchEnrollmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
import com.asms.springasms.repository.SubmissionRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import com.asms.springasms.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        if (userRepository.count() > 0) {
            log.info("Database already seeded, skipping DataSeeder.");
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

        userRepository.saveAll(List.of(adminUser, teacherUser, studentUser));

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

        batchRepository.saveAll(List.of(batchA, batchB));

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

        courseRepository.saveAll(List.of(course1, course2, course3));

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

        courseEnrollmentRepository.saveAll(List.of(courseEnrollment1, courseEnrollment2));

        // 7. Teacher Allocations
        TeacherCourseAllocation allocation1 = new TeacherCourseAllocation();
        allocation1.setTeacher(teacherUser);
        allocation1.setCourse(course1);
        allocation1.setStatus(TeacherCourseAllocationStatus.Active);

        TeacherCourseAllocation allocation2 = new TeacherCourseAllocation();
        allocation2.setTeacher(teacherUser);
        allocation2.setCourse(course2);
        allocation2.setStatus(TeacherCourseAllocationStatus.Active);

        allocationRepository.saveAll(List.of(allocation1, allocation2));

        // 8. Assignments
        Assignment assignment1 = new Assignment();
        assignment1.setCourse(course1);
        assignment1.setCreatedBy(teacherUser);
        assignment1.setTitle("Assignment 1: Variables and Control Flow");
        assignment1.setDescription("Implement basic control flow patterns and submit your solution.");
        assignment1.setDeadlineAt(Instant.now().plus(7, ChronoUnit.DAYS));
        assignment1.setMaximumMarks(new BigDecimal("100"));
        assignment1.setStatus(AssignmentStatus.Published);
        assignment1.setAllowResubmission(true);

        Assignment assignment2 = new Assignment();
        assignment2.setCourse(course2);
        assignment2.setCreatedBy(teacherUser);
        assignment2.setTitle("Assignment 2: Linked List Reversal (Draft)");
        assignment2.setDescription("Draft assignment for linked lists.");
        assignment2.setDeadlineAt(Instant.now().plus(14, ChronoUnit.DAYS));
        assignment2.setMaximumMarks(new BigDecimal("50"));
        assignment2.setStatus(AssignmentStatus.Draft);
        assignment2.setAllowResubmission(false);

        assignmentRepository.saveAll(List.of(assignment1, assignment2));

        // 9. Sample Submission
        Submission submission1 = new Submission();
        submission1.setAssignment(assignment1);
        submission1.setStudent(studentUser);
        submission1.setAnswerText("Here is my completed assignment for Variables and Control Flow.");
        submission1.setStatus(SubmissionStatus.Submitted);
        submission1.setSubmittedAt(Instant.now());
        submissionRepository.save(submission1);

        log.info("Database seeding completed: 3 users, 1 term, 2 batches, 3 courses, "
                + "1 batch enrollment, 2 course enrollments, 2 allocations, 2 assignments, 1 submission.");
    }
}
