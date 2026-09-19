package com.asms.springasms.dto.assignment;

import com.asms.springasms.entity.Assignment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        String courseCode,
        String title,
        String description,
        Instant deadlineAt,
        BigDecimal maximumMarks,
        String status,
        boolean allowResubmission,
        Instant submissionsClosedAt,
        UUID createdByUserId,
        String createdByName
) {
    public static AssignmentResponse from(Assignment a) {
        return new AssignmentResponse(a.getId(), a.getCourse().getId(),
                a.getCourse().getTitle(), a.getCourse().getCode(), a.getTitle(),
                a.getDescription(), a.getDeadlineAt(), a.getMaximumMarks(),
                a.getStatus().name(), a.isAllowResubmission(), a.getSubmissionsClosedAt(),
                a.getCreatedBy().getId(), a.getCreatedBy().getFullName());
    }
}
