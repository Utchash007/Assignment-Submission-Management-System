package com.asms.springasms.service;

import com.asms.springasms.dto.allocation.AllocateTeacherRequest;
import com.asms.springasms.dto.allocation.CourseTeacherResponse;
import com.asms.springasms.dto.allocation.SetAllocationStatusRequest;
import com.asms.springasms.dto.allocation.TeacherCourseResponse;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.TeacherCourseAllocation;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.CourseRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import com.asms.springasms.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeacherCourseAllocationService {

    private final TeacherCourseAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<CourseTeacherResponse> getCourseTeachers(UUID courseId) {
        return allocationRepository.findCourseTeachers(courseId).stream()
                .map(CourseTeacherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherCourseResponse> getTeacherCourses(UUID teacherId) {
        return allocationRepository.findTeacherCourses(teacherId).stream()
                .map(TeacherCourseResponse::from).toList();
    }

    @Transactional
    public CourseTeacherResponse allocate(AllocateTeacherRequest request) {
        User teacher = userRepository.findById(request.teacherId()).orElse(null);
        if (teacher == null || teacher.getRole() != UserRole.Teacher) {
            throw new IllegalStateException("Allocated user must exist and have the Teacher role.");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalStateException(
                        "Course with ID '" + request.courseId() + "' does not exist."));
        if (allocationRepository.existsByTeacherIdAndCourseId(request.teacherId(), request.courseId())) {
            throw new IllegalStateException("Teacher is already allocated to this course.");
        }
        TeacherCourseAllocation allocation = new TeacherCourseAllocation();
        allocation.setTeacher(teacher);
        allocation.setCourse(course);
        allocation.setStatus(TeacherCourseAllocationStatus.Active);
        return CourseTeacherResponse.from(allocationRepository.save(allocation));
    }

    @Transactional
    public void setStatus(UUID allocationId, SetAllocationStatusRequest request) {
        TeacherCourseAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new NotFoundException("Allocation Not Found",
                        "Teacher allocation with ID '" + allocationId + "' was not found."));
        allocation.setStatus(request.status());
    }
}
