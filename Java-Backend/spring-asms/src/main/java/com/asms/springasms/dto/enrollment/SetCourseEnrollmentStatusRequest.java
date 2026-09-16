package com.asms.springasms.dto.enrollment;

import com.asms.springasms.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record SetCourseEnrollmentStatusRequest(
        @NotNull EnrollmentStatus status
) {
}
