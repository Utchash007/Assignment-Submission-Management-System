package com.asms.springasms.dto.allocation;

import com.asms.springasms.entity.TeacherCourseAllocation;
import java.util.UUID;

public record CourseTeacherResponse(
        UUID allocationId,
        UUID teacherId,
        String teacherName,
        String teacherEmail,
        String status
) {
    public static CourseTeacherResponse from(TeacherCourseAllocation a) {
        return new CourseTeacherResponse(a.getId(), a.getTeacher().getId(),
                a.getTeacher().getFullName(), a.getTeacher().getEmail(), a.getStatus().name());
    }
}
