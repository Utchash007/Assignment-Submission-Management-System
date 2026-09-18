package com.asms.springasms.dto.assignment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID courseId,
        @NotBlank String title,
        String description,
        @NotNull Instant deadlineAt,
        @NotNull @DecimalMin("0.01") BigDecimal maximumMarks,
        Boolean allowResubmission
) {
}
