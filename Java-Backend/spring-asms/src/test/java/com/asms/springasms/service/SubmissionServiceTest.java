package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.submission.ReviewSubmissionRequest;
import com.asms.springasms.dto.submission.UpsertSubmissionRequest;
import com.asms.springasms.entity.Assignment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.Submission;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.AssignmentStatus;
import com.asms.springasms.enums.SubmissionStatus;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.SubmissionRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import com.asms.springasms.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private TeacherCourseAllocationRepository allocationRepository;
    @Mock
    private UserRepository userRepository;

    private SubmissionService submissionService() {
        return new SubmissionService(submissionRepository, assignmentRepository,
                enrollmentRepository, allocationRepository, userRepository);
    }

    private Course course() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setCode("CSE101");
        return course;
    }

    private Assignment assignment(boolean published, boolean closed, boolean resubmit) {
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setCourse(course());
        assignment.setTitle("Assignment 1");
        assignment.setDeadlineAt(Instant.now().plus(7, ChronoUnit.DAYS));
        assignment.setMaximumMarks(new BigDecimal("100"));
        assignment.setStatus(published ? AssignmentStatus.Published : AssignmentStatus.Draft);
        assignment.setAllowResubmission(resubmit);
        if (closed) {
            assignment.setSubmissionsClosedAt(Instant.now());
        }
        return assignment;
    }

    private User student(UUID id) {
        User student = new User();
        student.setId(id);
        student.setFullName("Demo Student");
        student.setRoll("S-1001");
        student.setRole(UserRole.Student);
        return student;
    }

    private void mockEnrolled(Assignment assignment, UUID studentId) {
        when(assignmentRepository.findActiveWithCourseById(assignment.getId()))
                .thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsActiveEnrollment(
                assignment.getCourse().getId(), studentId)).thenReturn(true);
    }

    @Test
    void upsert_newSubmission_shouldBeSubmitted() {
        Assignment assignment = assignment(true, false, true);
        UUID studentId = UUID.randomUUID();
        mockEnrolled(assignment, studentId);
        when(submissionRepository.findActiveByAssignmentIdAndStudentIdWithAttachments(
                assignment.getId(), studentId)).thenReturn(Optional.empty());
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(submissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var response = submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("answer"));

        assertEquals("Submitted", response.status());
        assertNull(response.marks());
        assertTrue(response.attachments().isEmpty());
    }

    @Test
    void upsert_pastDeadline_shouldBeLate() {
        Assignment assignment = assignment(true, false, true);
        assignment.setDeadlineAt(Instant.now().minus(1, ChronoUnit.HOURS));
        UUID studentId = UUID.randomUUID();
        mockEnrolled(assignment, studentId);
        when(submissionRepository.findActiveByAssignmentIdAndStudentIdWithAttachments(
                assignment.getId(), studentId)).thenReturn(Optional.empty());
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student(studentId)));
        when(submissionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertEquals("Late", submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("late"))
                .status());
    }

    @Test
    void upsert_draftAssignment_shouldThrowNotAvailable() {
        Assignment assignment = assignment(false, false, true);
        UUID studentId = UUID.randomUUID();
        when(assignmentRepository.findActiveWithCourseById(assignment.getId()))
                .thenReturn(Optional.of(assignment));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("x")));
        assertEquals("Assignment is not available for submissions.", ex.getMessage());
    }

    @Test
    void upsert_notEnrolled_shouldThrowForbidden() {
        Assignment assignment = assignment(true, false, true);
        UUID studentId = UUID.randomUUID();
        when(assignmentRepository.findActiveWithCourseById(assignment.getId()))
                .thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsActiveEnrollment(
                assignment.getCourse().getId(), studentId)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("x")));
        assertEquals("You are not enrolled in the course for this assignment.", ex.getMessage());
    }

    @Test
    void upsert_closedSubmissions_shouldThrowClosedMessage() {
        Assignment assignment = assignment(true, true, true);
        UUID studentId = UUID.randomUUID();
        mockEnrolled(assignment, studentId);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("x")));
        assertEquals("Submissions for this assignment have been closed by the teacher.",
                ex.getMessage());
    }

    @Test
    void upsert_resubmitDisallowed_shouldThrow() {
        Assignment assignment = assignment(true, false, false);
        UUID studentId = UUID.randomUUID();
        mockEnrolled(assignment, studentId);
        Submission existing = new Submission();
        existing.setAssignment(assignment);
        existing.setStudent(student(studentId));
        when(submissionRepository.findActiveByAssignmentIdAndStudentIdWithAttachments(
                assignment.getId(), studentId)).thenReturn(Optional.of(existing));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("v2")));
        assertEquals("Resubmission is not permitted for this assignment.", ex.getMessage());
    }

    @Test
    void upsert_resubmit_shouldKeepGradeButNullEvaluatorName() {
        Assignment assignment = assignment(true, false, true);
        UUID studentId = UUID.randomUUID();
        mockEnrolled(assignment, studentId);
        User evaluator = new User();
        evaluator.setId(UUID.randomUUID());
        evaluator.setFullName("Demo Teacher");
        Submission existing = new Submission();
        existing.setId(UUID.randomUUID());
        existing.setAssignment(assignment);
        existing.setStudent(student(studentId));
        existing.setMarks(new BigDecimal("60"));
        existing.setFeedback("Good");
        existing.setEvaluatedBy(evaluator);
        existing.setAttachments(List.of());
        when(submissionRepository.findActiveByAssignmentIdAndStudentIdWithAttachments(
                assignment.getId(), studentId)).thenReturn(Optional.of(existing));

        var response = submissionService()
                .upsert(assignment.getId(), studentId, new UpsertSubmissionRequest("v2"));

        assertEquals(new BigDecimal("60"), response.marks());
        assertEquals(evaluator.getId(), response.evaluatedByUserId());
        assertNull(response.evaluatedByName());
    }

    @Test
    void review_marksAboveMaximum_shouldThrowBoundsMessage() {
        Assignment assignment = assignment(true, false, true);
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setAssignment(assignment);
        submission.setStudent(student(UUID.randomUUID()));
        UUID teacherId = UUID.randomUUID();
        when(submissionRepository.findActiveByIdWithDetails(submission.getId()))
                .thenReturn(Optional.of(submission));
        when(allocationRepository.existsByTeacherIdAndCourseIdAndStatus(eq(teacherId),
                eq(assignment.getCourse().getId()), eq(TeacherCourseAllocationStatus.Active)))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> submissionService()
                .review(submission.getId(), teacherId, new ReviewSubmissionRequest(
                        new BigDecimal("101"), "fb", SubmissionStatus.Reviewed)));
        assertEquals("Marks must be between 0 and 100.", ex.getMessage());
    }

    @Test
    void review_unallocatedTeacher_shouldThrowForbidden() {
        Assignment assignment = assignment(true, false, true);
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setAssignment(assignment);
        submission.setStudent(student(UUID.randomUUID()));
        UUID teacherId = UUID.randomUUID();
        when(submissionRepository.findActiveByIdWithDetails(submission.getId()))
                .thenReturn(Optional.of(submission));
        when(allocationRepository.existsByTeacherIdAndCourseIdAndStatus(eq(teacherId),
                any(), any())).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> submissionService()
                .review(submission.getId(), teacherId, new ReviewSubmissionRequest(
                        new BigDecimal("80"), "fb", SubmissionStatus.Reviewed)));
        assertEquals("You are not allocated to the course for this submission.", ex.getMessage());
    }

    @Test
    void getForAssignment_unknown_shouldThrowBadRequest() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findActiveWithCourseById(assignmentId))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> submissionService()
                .getForAssignment(assignmentId, UUID.randomUUID(), UserRole.Teacher));
        assertEquals("Assignment with ID '" + assignmentId + "' was not found.", ex.getMessage());
    }

    @Test
    void getById_studentReadingOthers_shouldThrowForbidden() {
        Assignment assignment = assignment(true, false, true);
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setAssignment(assignment);
        submission.setStudent(student(UUID.randomUUID()));
        when(submissionRepository.findActiveByIdWithDetails(submission.getId()))
                .thenReturn(Optional.of(submission));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> submissionService()
                .getById(submission.getId(), UUID.randomUUID(), UserRole.Student));
        assertEquals("You are not authorized to access this submission.", ex.getMessage());
    }
}
