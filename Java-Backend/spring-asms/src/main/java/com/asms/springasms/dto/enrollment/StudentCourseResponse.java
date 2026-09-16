package com.asms.springasms.dto.enrollment;

import com.asms.springasms.entity.CourseEnrollment;
import java.util.UUID;

public record StudentCourseResponse(
        UUID enrollmentId,
        UUID courseId,
        String courseCode,
        String courseTitle,
        String status
) {
    public static StudentCourseResponse from(CourseEnrollment ce) {
        return new StudentCourseResponse(ce.getId(), ce.getCourse().getId(),
                ce.getCourse().getCode(), ce.getCourse().getTitle(), ce.getStatus().name());
    }
}
