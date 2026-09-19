package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.assignment.CreateAssignmentRequest;
import com.asms.springasms.dto.assignment.UpdateAssignmentRequest;
import com.asms.springasms.entity.Assignment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.AssignmentStatus;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
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
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TeacherCourseAllocationRepository allocationRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private SubmissionRepository submissionRepository;

    private AssignmentService assignmentService() {
        return new AssignmentService(assignmentRepository, courseRepository, userRepository,
                allocationRepository, enrollmentRepository, submissionRepository);
    }

    private Course course() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setCode("CSE101");
        course.setTitle("Introduction to Programming");
        return course;
    }

    private User teacher(UUID id) {
        User teacher = new User();
        teacher.setId(id);
        teacher.setFullName("Demo Teacher");
        teacher.setRole(UserRole.Teacher);
        return teacher;
    }

    private Assignment assignment(Course course, User creator, AssignmentStatus status) {
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setCourse(course);
        assignment.setCreatedBy(creator);
        assignment.setTitle("Assignment 1");
        assignment.setDeadlineAt(Instant.now().plus(7, ChronoUnit.DAYS));
        assignment.setMaximumMarks(new BigDecimal("100"));
        assignment.setStatus(status);
        return assignment;
    }

    @Test
    void create_unallocatedTeacher_shouldThrowForbidden() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(allocationRepository.existsByTeacherIdAndCourseIdAndStatus(teacherId, courseId,
                TeacherCourseAllocationStatus.Active)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assignmentService()
                .create(teacherId, new CreateAssignmentRequest(courseId, "T", null,
                        Instant.now().plus(7, ChronoUnit.DAYS), new BigDecimal("100"), false)));
        assertEquals("Teacher does not have an active allocation to this course.", ex.getMessage());
    }

    @Test
    void create_unknownCourse_shouldThrowDoesNotExist() {
        UUID teacherId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(allocationRepository.existsByTeacherIdAndCourseIdAndStatus(any(), any(), any()))
                .thenReturn(true);
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> assignmentService()
                .create(teacherId, new CreateAssignmentRequest(courseId, "T", null,
                        Instant.now().plus(7, ChronoUnit.DAYS), new BigDecimal("100"), false)));
        assertEquals("Course with ID '" + courseId + "' does not exist.", ex.getMessage());
    }

    @Test
    void create_success_shouldStartAsDraft() {
        UUID teacherId = UUID.randomUUID();
        Course course = course();
        when(allocationRepository.existsByTeacherIdAndCourseIdAndStatus(any(), any(), any()))
                .thenReturn(true);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher(teacherId)));
        when(assignmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var created = assignmentService().create(teacherId, new CreateAssignmentRequest(
                course.getId(), "  T  ", "   ", Instant.now().plus(7, ChronoUnit.DAYS),
                new BigDecimal("100"), true));

        assertEquals("Draft", created.status());
        assertEquals("T", created.title());
        assertNull(created.description());
    }

    @Test
    void update_nonCreator_shouldThrowForbidden() {
        Course course = course();
        User creator = teacher(UUID.randomUUID());
        Assignment assignment = assignment(course, creator, AssignmentStatus.Draft);
        UUID otherTeacher = UUID.randomUUID();
        when(assignmentRepository.findActiveWithDetailsById(assignment.getId()))
                .thenReturn(Optional.of(assignment));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assignmentService()
                .update(assignment.getId(), otherTeacher, new UpdateAssignmentRequest("T", null,
                        Instant.now(), new BigDecimal("10"), false)));
        assertEquals("Only the teacher who created this assignment can modify it.", ex.getMessage());
    }

    @Test
    void update_publishedWithSubmissions_shouldThrow() {
        Course course = course();
        User creator = teacher(UUID.randomUUID());
        Assignment assignment = assignment(course, creator, AssignmentStatus.Published);
        when(assignmentRepository.findActiveWithDetailsById(assignment.getId()))
                .thenReturn(Optional.of(assignment));
        when(submissionRepository.existsActiveByAssignmentId(assignment.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> assignmentService()
                .update(assignment.getId(), creator.getId(), new UpdateAssignmentRequest("T", null,
                        Instant.now(), new BigDecimal("10"), false)));
        assertEquals("Cannot modify assignment details because student submissions already exist.",
                ex.getMessage());
    }

    @Test
    void publish_missing_shouldThrowAssignmentNotFound() {
        UUID id = UUID.randomUUID();
        when(assignmentRepository.findActiveById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> assignmentService().publish(id, UUID.randomUUID()));
        assertEquals("Assignment Not Found", ex.getTitle());
    }

    @Test
    void closeSubmissions_success_shouldTimestamp() {
        Course course = course();
        User creator = teacher(UUID.randomUUID());
        Assignment assignment = assignment(course, creator, AssignmentStatus.Published);
        when(assignmentRepository.findActiveById(assignment.getId()))
                .thenReturn(Optional.of(assignment));

        assignmentService().closeSubmissions(assignment.getId(), creator.getId());

        assertNotNull(assignment.getSubmissionsClosedAt());
    }

    @Test
    void delete_nonCreator_shouldThrowForbidden() {
        Course course = course();
        User creator = teacher(UUID.randomUUID());
        Assignment assignment = assignment(course, creator, AssignmentStatus.Draft);
        when(assignmentRepository.findActiveById(assignment.getId()))
                .thenReturn(Optional.of(assignment));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assignmentService()
                .delete(assignment.getId(), UUID.randomUUID()));
        assertEquals("Only the teacher who created this assignment can delete it.", ex.getMessage());
        assertNull(assignment.getDeletedAt());
    }

    @Test
    void getById_studentUnpublished_shouldThrowNotFound() {
        Course course = course();
        Assignment assignment = assignment(course, teacher(UUID.randomUUID()), AssignmentStatus.Draft);
        when(assignmentRepository.findActiveWithDetailsById(assignment.getId()))
                .thenReturn(Optional.of(assignment));

        assertThrows(NotFoundException.class, () -> assignmentService()
                .getById(assignment.getId(), UUID.randomUUID(), UserRole.Student));
    }

    @Test
    void getAssignments_teacherWithoutAllocations_shouldReturnEmptyWithoutQuery() {
        UUID teacherId = UUID.randomUUID();
        when(allocationRepository.findActiveCourseIdsByTeacherId(teacherId))
                .thenReturn(List.of());

        assertTrue(assignmentService()
                .getAssignments(teacherId, UserRole.Teacher, null).isEmpty());
        verify(assignmentRepository, never()).findActiveByCourseIdInWithDetails(any());
    }
}
