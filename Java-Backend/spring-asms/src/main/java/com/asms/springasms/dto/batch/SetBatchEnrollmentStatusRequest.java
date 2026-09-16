package com.asms.springasms.dto.batch;

import com.asms.springasms.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record SetBatchEnrollmentStatusRequest(
        @NotNull EnrollmentStatus status
) {
}
