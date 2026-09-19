package com.asms.springasms.dto.enrollment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record EnrollStudentsRequest(
        @NotNull UUID courseId,
        @NotEmpty List<UUID> batchEnrollmentIds
) {
}
