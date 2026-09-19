package com.asms.springasms.dto.batch;

import jakarta.validation.constraints.NotBlank;

public record UpdateBatchRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
