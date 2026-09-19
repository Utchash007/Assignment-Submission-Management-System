package com.asms.springasms.dto.term;

import com.asms.springasms.entity.AcademicTerm;
import java.time.LocalDate;
import java.util.UUID;

public record AcademicTermResponse(
        UUID id,
        String code,
        LocalDate startsOn,
        LocalDate endsOn
) {
    public static AcademicTermResponse from(AcademicTerm term) {
        return new AcademicTermResponse(term.getId(), term.getCode(), term.getStartsOn(), term.getEndsOn());
    }
}
