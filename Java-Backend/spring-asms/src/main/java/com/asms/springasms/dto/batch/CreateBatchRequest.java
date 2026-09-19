package com.asms.springasms.dto.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateBatchRequest(
        @NotNull UUID termId,
        @NotBlank String code,
        @NotBlank String name
) {
}
