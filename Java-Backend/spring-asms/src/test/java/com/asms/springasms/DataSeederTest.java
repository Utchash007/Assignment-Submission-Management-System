package com.asms.springasms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

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
import com.asms.springasms.seed.DataSeeder;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Phase 6 verification — DataSeeder logic without any database.
 * (A live fresh-DB run is intentionally NOT done here: there is no local
 * PostgreSQL, and the shared Supabase database must never be mutated. The
 * seeder's guard path is additionally verified by booting against Supabase,
 * where it must log a skip and change nothing.)
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private AcademicTermRepository termRepository;
    @Mock private AcademicBatchRepository batchRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private BatchEnrollmentRepository batchEnrollmentRepository;
    @Mock private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock private TeacherCourseAllocationRepository allocationRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private SubmissionRepository submissionRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private DataSeeder dataSeeder;

    @BeforeEach
    void setUp() {
        dataSeeder = new DataSeeder(userRepository, termRepository, batchRepository, courseRepository,
                batchEnrollmentRepository, courseEnrollmentRepository, allocationRepository,
                assignmentRepository, submissionRepository, passwordEncoder);
    }

    @Test
    @SuppressWarnings("unchecked")
    void run_emptyDatabase_shouldSeedFullDemoDataset() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        dataSeeder.run(null);

        ArgumentCaptor<List<User>> usersCaptor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).saveAll(usersCaptor.capture());
        List<User> users = usersCaptor.getValue();
        assertEquals(3, users.size());

        User admin = byEmail(users, "admin@onnorokom.com");
        assertEquals("System Admin", admin.getFullName());
        assertEquals(UserRole.Admin, admin.getRole());
        assertTrue(admin.isActive());
        assertTrue(passwordEncoder.matches("Admin@123", admin.getPasswordHash()));

        User teacher = byEmail(users, "teacher@onnorokom.com");
        assertEquals("Demo Teacher", teacher.getFullName());
        assertEquals(UserRole.Teacher, teacher.getRole());
        assertTrue(passwordEncoder.matches("Teacher@123", teacher.getPasswordHash()));

        User student = byEmail(users, "student@onnorokom.com");
        assertEquals("S-1001", student.getRoll());
        assertEquals(UserRole.Student, student.getRole());
        assertTrue(passwordEncoder.matches("Student@123", student.getPasswordHash()));

        ArgumentCaptor<AcademicTerm> termCaptor = ArgumentCaptor.forClass(AcademicTerm.class);
        verify(termRepository).save(termCaptor.capture());
        assertEquals("FALL2026", termCaptor.getValue().getCode());
        assertEquals(java.time.LocalDate.of(2026, 9, 1), termCaptor.getValue().getStartsOn());
        assertEquals(java.time.LocalDate.of(2026, 12, 31), termCaptor.getValue().getEndsOn());

        ArgumentCaptor<List<AcademicBatch>> batchesCaptor = ArgumentCaptor.forClass(List.class);
        verify(batchRepository).saveAll(batchesCaptor.capture());
        assertEquals(List.of("BATCH-2026-A", "BATCH-2026-B"),
                batchesCaptor.getValue().stream().map(AcademicBatch::getCode).toList());
        assertTrue(batchesCaptor.getValue().stream()
                .allMatch(b -> b.getTerm() == termCaptor.getValue()));

        ArgumentCaptor<List<Course>> coursesCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseRepository).saveAll(coursesCaptor.capture());
        assertEquals(List.of("CSE101", "CSE102", "CSE103"),
                coursesCaptor.getValue().stream().map(Course::getCode).toList());

        ArgumentCaptor<BatchEnrollment> beCaptor = ArgumentCaptor.forClass(BatchEnrollment.class);
        verify(batchEnrollmentRepository).save(beCaptor.capture());
        assertEquals(EnrollmentStatus.Active, beCaptor.getValue().getStatus());

        ArgumentCaptor<List<CourseEnrollment>> ceCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseEnrollmentRepository).saveAll(ceCaptor.capture());
        assertEquals(2, ceCaptor.getValue().size());
        assertTrue(ceCaptor.getValue().stream()
                .allMatch(ce -> ce.getStatus() == EnrollmentStatus.Active));

        ArgumentCaptor<List<TeacherCourseAllocation>> allocCaptor = ArgumentCaptor.forClass(List.class);
        verify(allocationRepository).saveAll(allocCaptor.capture());
        assertEquals(2, allocCaptor.getValue().size());
        assertTrue(allocCaptor.getValue().stream()
                .allMatch(a -> a.getStatus() == TeacherCourseAllocationStatus.Active));

        ArgumentCaptor<List<Assignment>> assignmentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).saveAll(assignmentsCaptor.capture());
        List<Assignment> assignments = assignmentsCaptor.getValue();
        assertEquals(2, assignments.size());
        Assignment published = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.Published).findFirst().orElseThrow();
        assertEquals("Assignment 1: Variables and Control Flow", published.getTitle());
        assertTrue(published.isAllowResubmission());
        Assignment draft = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.Draft).findFirst().orElseThrow();
        assertEquals("Assignment 2: Linked List Reversal (Draft)", draft.getTitle());
        assertFalse(draft.isAllowResubmission());

        ArgumentCaptor<Submission> submissionCaptor = ArgumentCaptor.forClass(Submission.class);
        verify(submissionRepository).save(submissionCaptor.capture());
        assertEquals(SubmissionStatus.Submitted, submissionCaptor.getValue().getStatus());
        assertNotNull(submissionCaptor.getValue().getAnswerText());
    }

    @Test
    void run_populatedDatabase_shouldBeNoOp() throws Exception {
        when(userRepository.count()).thenReturn(4L);

        dataSeeder.run(null);

        verify(userRepository, never()).saveAll(anyList());
        verifyNoInteractions(termRepository, batchRepository, courseRepository,
                batchEnrollmentRepository, courseEnrollmentRepository, allocationRepository,
                assignmentRepository, submissionRepository);
    }

    private static User byEmail(List<User> users, String email) {
        return users.stream().filter(u -> u.getEmail().equals(email)).findFirst().orElseThrow();
    }
}
