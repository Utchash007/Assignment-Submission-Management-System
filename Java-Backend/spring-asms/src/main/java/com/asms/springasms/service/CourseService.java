package com.asms.springasms.service;

import com.asms.springasms.dto.course.CourseResponse;
import com.asms.springasms.dto.course.CreateCourseRequest;
import com.asms.springasms.dto.course.UpdateCourseRequest;
import com.asms.springasms.entity.Course;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final TeacherCourseAllocationRepository allocationRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> getAll() {
        return courseRepository.findAllByOrderByCodeAsc().stream()
                .map(CourseResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(UUID id) {
        return courseRepository.findById(id)
                .map(CourseResponse::from)
                .orElseThrow(() -> new NotFoundException("Course Not Found",
                        "Course with ID '" + id + "' was not found."));
    }

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        String normalizedCode = request.code().trim().toUpperCase();
        if (courseRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalStateException(
                    "Course with code '" + request.code() + "' already exists.");
        }
        Course course = new Course();
        course.setCode(normalizedCode);
        course.setTitle(request.title().trim());
        course.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        return CourseResponse.from(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse update(UUID id, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course Not Found",
                        "Course with ID '" + id + "' was not found."));
        String normalizedCode = request.code().trim().toUpperCase();
        if (!course.getCode().equalsIgnoreCase(normalizedCode)
                && courseRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new IllegalStateException(
                    "Course with code '" + request.code() + "' already exists.");
        }
        course.setCode(normalizedCode);
        course.setTitle(request.title().trim());
        course.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        return CourseResponse.from(course);
    }

    @Transactional
    public void delete(UUID id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course Not Found",
                        "Course with ID '" + id + "' was not found."));
        if (allocationRepository.existsByCourseId(id)
                || enrollmentRepository.existsByCourseId(id)
                || assignmentRepository.existsByCourseIdIncludingDeleted(id)) {
            throw new IllegalStateException(
                    "Cannot delete course because teacher allocations, student enrollments, or assignments exist.");
        }
        courseRepository.delete(course);
    }
}
