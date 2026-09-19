package com.asms.springasms.dto.course;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description
) {
}
