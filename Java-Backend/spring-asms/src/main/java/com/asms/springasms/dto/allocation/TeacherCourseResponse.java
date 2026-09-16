package com.asms.springasms.dto.allocation;

import com.asms.springasms.entity.TeacherCourseAllocation;
import java.util.UUID;

public record TeacherCourseResponse(
        UUID allocationId,
        UUID courseId,
        String courseCode,
        String courseTitle,
        String status
) {
    public static TeacherCourseResponse from(TeacherCourseAllocation a) {
        return new TeacherCourseResponse(a.getId(), a.getCourse().getId(),
                a.getCourse().getCode(), a.getCourse().getTitle(), a.getStatus().name());
    }
}
