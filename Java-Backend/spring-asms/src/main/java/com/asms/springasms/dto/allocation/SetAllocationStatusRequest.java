package com.asms.springasms.dto.allocation;

import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import jakarta.validation.constraints.NotNull;

public record SetAllocationStatusRequest(
        @NotNull TeacherCourseAllocationStatus status
) {
}
