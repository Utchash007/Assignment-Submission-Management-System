package com.asms.springasms.controller;

import com.asms.springasms.dto.allocation.AllocateTeacherRequest;
import com.asms.springasms.dto.allocation.CourseTeacherResponse;
import com.asms.springasms.dto.allocation.SetAllocationStatusRequest;
import com.asms.springasms.dto.allocation.TeacherCourseResponse;
import com.asms.springasms.service.TeacherCourseAllocationService;
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
@RequestMapping("/api/teacher-allocations")
@RequiredArgsConstructor
public class TeacherCourseAllocationsController {

    private final TeacherCourseAllocationService allocationService;

    @GetMapping("/courses/{courseId}/teachers")
    public ResponseEntity<List<CourseTeacherResponse>> getCourseTeachers(@PathVariable UUID courseId) {
        return ResponseEntity.ok(allocationService.getCourseTeachers(courseId));
    }

    @GetMapping("/teachers/{teacherId}/courses")
    public ResponseEntity<List<TeacherCourseResponse>> getTeacherCourses(@PathVariable UUID teacherId) {
        return ResponseEntity.ok(allocationService.getTeacherCourses(teacherId));
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<CourseTeacherResponse> allocate(@Valid @RequestBody AllocateTeacherRequest request) {
        CourseTeacherResponse created = allocationService.allocate(request);
        return ResponseEntity.created(URI.create(
                "api/teacher-allocations/courses/" + request.courseId() + "/teachers")).body(created);
    }

    @PatchMapping("/{allocationId}/status")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> setStatus(@PathVariable UUID allocationId,
                                          @Valid @RequestBody SetAllocationStatusRequest request) {
        allocationService.setStatus(allocationId, request);
        return ResponseEntity.noContent().build();
    }
}
