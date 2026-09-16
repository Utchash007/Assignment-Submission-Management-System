package com.asms.springasms.controller;

import com.asms.springasms.dto.enrollment.CourseStudentResponse;
import com.asms.springasms.dto.enrollment.EnrollStudentsRequest;
import com.asms.springasms.dto.enrollment.SetCourseEnrollmentStatusRequest;
import com.asms.springasms.dto.enrollment.StudentCourseResponse;
import com.asms.springasms.service.CourseEnrollmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-enrollments")
@RequiredArgsConstructor
public class CourseEnrollmentsController {

    private final CourseEnrollmentService enrollmentService;

    @GetMapping("/courses/{courseId}/students")
    @PreAuthorize("hasAnyRole('Admin','Teacher')")
    public ResponseEntity<List<CourseStudentResponse>> getCourseStudents(@PathVariable UUID courseId) {
        return ResponseEntity.ok(enrollmentService.getCourseStudents(courseId));
    }

    @GetMapping("/students/{studentId}/courses")
    public ResponseEntity<List<StudentCourseResponse>> getStudentCourses(@PathVariable UUID studentId) {
        return ResponseEntity.ok(enrollmentService.getStudentCourses(studentId));
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<List<CourseStudentResponse>> enroll(@Valid @RequestBody EnrollStudentsRequest request) {
        List<CourseStudentResponse> roster = enrollmentService.enrollStudents(request);
        return ResponseEntity.created(URI.create(
                "api/course-enrollments/courses/" + request.courseId() + "/students")).body(roster);
    }

    @PatchMapping("/{enrollmentId}/status")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> setStatus(@PathVariable UUID enrollmentId,
                                          @Valid @RequestBody SetCourseEnrollmentStatusRequest request) {
        enrollmentService.setStatus(enrollmentId, request);
        return ResponseEntity.noContent().build();
    }
}
