package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.enrollment.EnrollStudentsRequest;
import com.asms.springasms.dto.enrollment.SetCourseEnrollmentStatusRequest;
import com.asms.springasms.entity.BatchEnrollment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.CourseEnrollment;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.EnrollmentStatus;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.BatchEnrollmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentServiceTest {

    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private BatchEnrollmentRepository batchEnrollmentRepository;

    private CourseEnrollmentService enrollmentService() {
        return new CourseEnrollmentService(enrollmentRepository, courseRepository,
                batchEnrollmentRepository);
    }

    private Course course() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setCode("CSE101");
        return course;
    }

    private BatchEnrollment batchEnrollment(String studentName) {
        User student = new User();
        student.setId(UUID.randomUUID());
        student.setFullName(studentName);
        com.asms.springasms.entity.AcademicBatch batch =
                new com.asms.springasms.entity.AcademicBatch();
        batch.setId(UUID.randomUUID());
        batch.setCode("BATCH-2026-A");
        BatchEnrollment be = new BatchEnrollment();
        be.setId(UUID.randomUUID());
        be.setBatch(batch);
        be.setStudent(student);
        be.setStatus(EnrollmentStatus.Active);
        return be;
    }

    @Test
    void enroll_unknownCourse_shouldThrowDoesNotExist() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> enrollmentService()
                .enrollStudents(new EnrollStudentsRequest(courseId, List.of(UUID.randomUUID()))));
        assertEquals("Course with ID '" + courseId + "' does not exist.", ex.getMessage());
    }

    @Test
    void enroll_missingBatchEnrollments_shouldThrowCouldNotBeFound() {
        Course course = course();
        UUID missing = UUID.randomUUID();
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(batchEnrollmentRepository.findAllByIdInWithStudentAndBatch(List.of(missing)))
                .thenReturn(List.of());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> enrollmentService()
                .enrollStudents(new EnrollStudentsRequest(course.getId(), List.of(missing))));
        assertEquals("One or more specified batch enrollments could not be found.", ex.getMessage());
    }

    @Test
    void enroll_inactiveBatchEnrollment_shouldThrowWithStudentName() {
        Course course = course();
        BatchEnrollment be = batchEnrollment("Sleepy Student");
        be.setStatus(EnrollmentStatus.Inactive);
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(batchEnrollmentRepository.findAllByIdInWithStudentAndBatch(List.of(be.getId())))
                .thenReturn(List.of(be));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> enrollmentService()
                .enrollStudents(new EnrollStudentsRequest(course.getId(), List.of(be.getId()))));
        assertEquals("Student 'Sleepy Student' does not have an active batch enrollment.",
                ex.getMessage());
    }

    @Test
    void enroll_allDuplicates_shouldSkipWriteButReturnRoster() {
        Course course = course();
        BatchEnrollment be = batchEnrollment("Demo Student");
        CourseEnrollment existing = new CourseEnrollment();
        existing.setBatchEnrollment(be);
        existing.setCourse(course);
        UUID courseId = course.getId();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(batchEnrollmentRepository.findAllByIdInWithStudentAndBatch(List.of(be.getId())))
                .thenReturn(List.of(be));
        when(enrollmentRepository.findByCourseIdAndBatchEnrollmentIdIn(courseId, List.of(be.getId())))
                .thenReturn(List.of(existing));
        when(enrollmentRepository.findCourseStudents(courseId)).thenReturn(List.of(existing));

        var roster = enrollmentService().enrollStudents(
                new EnrollStudentsRequest(courseId, List.of(be.getId(), be.getId())));

        verify(enrollmentRepository, never()).saveAll(any());
        assertEquals(1, roster.size());
    }

    @Test
    void setStatus_unknown_shouldThrowEnrollmentNotFound() {
        UUID id = UUID.randomUUID();
        when(enrollmentRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> enrollmentService()
                .setStatus(id, new SetCourseEnrollmentStatusRequest(EnrollmentStatus.Inactive)));
        assertEquals("Course Enrollment Not Found", ex.getTitle());
    }
}
