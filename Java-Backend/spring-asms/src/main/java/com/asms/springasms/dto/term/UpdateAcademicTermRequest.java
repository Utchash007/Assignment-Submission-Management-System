package com.asms.springasms.dto.term;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record UpdateAcademicTermRequest(
        @NotBlank String code,
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn
) {
}
