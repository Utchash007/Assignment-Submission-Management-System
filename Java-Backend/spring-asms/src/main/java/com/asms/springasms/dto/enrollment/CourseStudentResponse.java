package com.asms.springasms.dto.enrollment;

import com.asms.springasms.entity.CourseEnrollment;
import java.util.UUID;

public record CourseStudentResponse(
        UUID enrollmentId,
        UUID studentId,
        String studentName,
        String studentEmail,
        String studentRoll,
        String batchCode,
        String status
) {
    public static CourseStudentResponse from(CourseEnrollment ce) {
        return new CourseStudentResponse(ce.getId(), ce.getBatchEnrollment().getStudent().getId(),
                ce.getBatchEnrollment().getStudent().getFullName(),
                ce.getBatchEnrollment().getStudent().getEmail(),
                ce.getBatchEnrollment().getStudent().getRoll(),
                ce.getBatchEnrollment().getBatch().getCode(), ce.getStatus().name());
    }
}
