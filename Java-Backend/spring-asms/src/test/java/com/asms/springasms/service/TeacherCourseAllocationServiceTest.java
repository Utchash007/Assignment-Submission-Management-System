package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.allocation.AllocateTeacherRequest;
import com.asms.springasms.dto.allocation.SetAllocationStatusRequest;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.TeacherCourseAllocation;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.CourseRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import com.asms.springasms.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeacherCourseAllocationServiceTest {

    @Mock
    private TeacherCourseAllocationRepository allocationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;

    private TeacherCourseAllocationService allocationService() {
        return new TeacherCourseAllocationService(allocationRepository, userRepository,
                courseRepository);
    }

    private User teacher() {
        User teacher = new User();
        teacher.setId(UUID.randomUUID());
        teacher.setFullName("Demo Teacher");
        teacher.setEmail("teacher@onnorokom.com");
        teacher.setRole(UserRole.Teacher);
        return teacher;
    }

    @Test
    void allocate_nonTeacher_shouldThrowRoleMessage() {
        User student = teacher();
        student.setRole(UserRole.Student);
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> allocationService()
                .allocate(new AllocateTeacherRequest(student.getId(), UUID.randomUUID())));
        assertEquals("Allocated user must exist and have the Teacher role.", ex.getMessage());
    }

    @Test
    void allocate_unknownCourse_shouldThrowDoesNotExist() {
        User teacher = teacher();
        UUID courseId = UUID.randomUUID();
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> allocationService()
                .allocate(new AllocateTeacherRequest(teacher.getId(), courseId)));
        assertEquals("Course with ID '" + courseId + "' does not exist.", ex.getMessage());
    }

    @Test
    void allocate_duplicate_shouldThrowAlreadyAllocated() {
        User teacher = teacher();
        Course course = new Course();
        course.setId(UUID.randomUUID());
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(allocationRepository.existsByTeacherIdAndCourseId(teacher.getId(), course.getId()))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> allocationService()
                .allocate(new AllocateTeacherRequest(teacher.getId(), course.getId())));
        assertEquals("Teacher is already allocated to this course.", ex.getMessage());
    }

    @Test
    void allocate_success_shouldForceActive() {
        User teacher = teacher();
        Course course = new Course();
        course.setId(UUID.randomUUID());
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(allocationRepository.existsByTeacherIdAndCourseId(any(), any())).thenReturn(false);
        when(allocationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var created = allocationService()
                .allocate(new AllocateTeacherRequest(teacher.getId(), course.getId()));

        assertEquals("Active", created.status());
        assertEquals("Demo Teacher", created.teacherName());
    }

    @Test
    void setStatus_unknown_shouldThrowAllocationNotFound() {
        UUID id = UUID.randomUUID();
        when(allocationRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> allocationService()
                .setStatus(id, new SetAllocationStatusRequest(TeacherCourseAllocationStatus.Inactive)));
        assertEquals("Allocation Not Found", ex.getTitle());
    }

    @Test
    void setStatus_success_shouldUpdate() {
        TeacherCourseAllocation allocation = new TeacherCourseAllocation();
        allocation.setId(UUID.randomUUID());
        allocation.setStatus(TeacherCourseAllocationStatus.Active);
        when(allocationRepository.findById(allocation.getId()))
                .thenReturn(Optional.of(allocation));

        allocationService().setStatus(allocation.getId(),
                new SetAllocationStatusRequest(TeacherCourseAllocationStatus.Inactive));

        assertEquals(TeacherCourseAllocationStatus.Inactive, allocation.getStatus());
    }
}
