package com.asms.springasms.dto.allocation;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AllocateTeacherRequest(
        @NotNull UUID teacherId,
        @NotNull UUID courseId
) {
}
