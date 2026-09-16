package com.asms.springasms.service;

import com.asms.springasms.dto.enrollment.CourseStudentResponse;
import com.asms.springasms.dto.enrollment.EnrollStudentsRequest;
import com.asms.springasms.dto.enrollment.SetCourseEnrollmentStatusRequest;
import com.asms.springasms.dto.enrollment.StudentCourseResponse;
import com.asms.springasms.entity.BatchEnrollment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.CourseEnrollment;
import com.asms.springasms.enums.EnrollmentStatus;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.BatchEnrollmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final BatchEnrollmentRepository batchEnrollmentRepository;

    @Transactional(readOnly = true)
    public List<CourseStudentResponse> getCourseStudents(UUID courseId) {
        return enrollmentRepository.findCourseStudents(courseId).stream()
                .map(CourseStudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentCourseResponse> getStudentCourses(UUID studentId) {
        return enrollmentRepository.findStudentCourses(studentId).stream()
                .map(StudentCourseResponse::from).toList();
    }

    @Transactional
    public List<CourseStudentResponse> enrollStudents(EnrollStudentsRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalStateException(
                        "Course with ID '" + request.courseId() + "' does not exist."));
        List<UUID> distinctIds = request.batchEnrollmentIds().stream().distinct().toList();
        List<BatchEnrollment> batchEnrollments =
                batchEnrollmentRepository.findAllByIdInWithStudentAndBatch(distinctIds);
        if (batchEnrollments.size() != distinctIds.size()) {
            throw new IllegalStateException("One or more specified batch enrollments could not be found.");
        }
        for (BatchEnrollment be : batchEnrollments) {
            if (be.getStatus() != EnrollmentStatus.Active) {
                throw new IllegalStateException("Student '" + be.getStudent().getFullName()
                        + "' does not have an active batch enrollment.");
            }
        }
        Map<UUID, CourseEnrollment> existing = enrollmentRepository
                .findByCourseIdAndBatchEnrollmentIdIn(request.courseId(), distinctIds).stream()
                .collect(Collectors.toMap(e -> e.getBatchEnrollment().getId(), Function.identity()));
        List<CourseEnrollment> toAdd = new java.util.ArrayList<>();
        for (BatchEnrollment be : batchEnrollments) {
            if (!existing.containsKey(be.getId())) {
                CourseEnrollment ce = new CourseEnrollment();
                ce.setBatchEnrollment(be);
                ce.setCourse(course);
                ce.setStatus(EnrollmentStatus.Active);
                toAdd.add(ce);
            }
        }
        if (!toAdd.isEmpty()) {
            enrollmentRepository.saveAll(toAdd);
        }
        return getCourseStudents(request.courseId());
    }

    @Transactional
    public void setStatus(UUID enrollmentId, SetCourseEnrollmentStatusRequest request) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Course Enrollment Not Found",
                        "Course enrollment with ID '" + enrollmentId + "' was not found."));
        enrollment.setStatus(request.status());
    }
}
