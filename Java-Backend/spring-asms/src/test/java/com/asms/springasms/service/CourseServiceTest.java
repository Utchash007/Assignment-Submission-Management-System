package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.course.CreateCourseRequest;
import com.asms.springasms.dto.course.UpdateCourseRequest;
import com.asms.springasms.entity.Course;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TeacherCourseAllocationRepository allocationRepository;
    @Mock
    private CourseEnrollmentRepository enrollmentRepository;
    @Mock
    private AssignmentRepository assignmentRepository;

    private CourseService courseService() {
        return new CourseService(courseRepository, allocationRepository,
                enrollmentRepository, assignmentRepository);
    }

    private Course course() {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setCode("CSE101");
        course.setTitle("Introduction to Programming");
        return course;
    }

    @Test
    void create_duplicateCode_shouldThrowWithRawCode() {
        when(courseRepository.existsByCodeIgnoreCase("CSE101")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> courseService()
                .create(new CreateCourseRequest(" cse101 ", "T", null)));
        assertEquals("Course with code ' cse101 ' already exists.", ex.getMessage());
    }

    @Test
    void update_unknown_shouldThrowCourseNotFound() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> courseService()
                .update(id, new UpdateCourseRequest("CSE101", "T", null)));
        assertEquals("Course Not Found", ex.getTitle());
    }

    @Test
    void delete_withAllocation_shouldThrowGuardMessage() {
        Course course = course();
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(allocationRepository.existsByCourseId(course.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> courseService().delete(course.getId()));
        assertEquals("Cannot delete course because teacher allocations, student enrollments, "
                + "or assignments exist.", ex.getMessage());
        verify(courseRepository, never()).delete(any());
    }

    @Test
    void delete_softDeletedAssignmentStillBlocks_shouldThrow() {
        Course course = course();
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(allocationRepository.existsByCourseId(any())).thenReturn(false);
        when(enrollmentRepository.existsByCourseId(any())).thenReturn(false);
        when(assignmentRepository.existsByCourseIdIncludingDeleted(course.getId())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> courseService().delete(course.getId()));
    }

    @Test
    void delete_clean_shouldDelete() {
        Course course = course();
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(allocationRepository.existsByCourseId(any())).thenReturn(false);
        when(enrollmentRepository.existsByCourseId(any())).thenReturn(false);
        when(assignmentRepository.existsByCourseIdIncludingDeleted(any())).thenReturn(false);

        courseService().delete(course.getId());

        verify(courseRepository).delete(course);
    }
}
