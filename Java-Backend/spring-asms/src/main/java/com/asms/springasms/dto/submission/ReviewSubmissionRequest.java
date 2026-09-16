package com.asms.springasms.dto.submission;

import com.asms.springasms.enums.SubmissionStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReviewSubmissionRequest(
        @NotNull @DecimalMin("0") BigDecimal marks,
        String feedback,
        @NotNull SubmissionStatus status
) {
}
