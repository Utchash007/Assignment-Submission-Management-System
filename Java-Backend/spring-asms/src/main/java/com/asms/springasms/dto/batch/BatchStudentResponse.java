package com.asms.springasms.dto.batch;

import com.asms.springasms.entity.BatchEnrollment;
import java.util.UUID;

public record BatchStudentResponse(
        UUID enrollmentId,
        UUID studentId,
        String studentName,
        String studentEmail,
        String studentRoll,
        String status
) {
    public static BatchStudentResponse from(BatchEnrollment e) {
        return new BatchStudentResponse(e.getId(), e.getStudent().getId(),
                e.getStudent().getFullName(), e.getStudent().getEmail(),
                e.getStudent().getRoll(), e.getStatus().name());
    }
}
