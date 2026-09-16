package com.asms.springasms.dto.course;

import jakarta.validation.constraints.NotBlank;

public record UpdateCourseRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description
) {
}
