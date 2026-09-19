package com.asms.springasms.dto.batch;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignStudentRequest(
        @NotNull UUID studentId
) {
}
