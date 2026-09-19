package com.asms.springasms.dto.batch;

import com.asms.springasms.entity.AcademicBatch;
import java.util.UUID;

public record BatchResponse(
        UUID id,
        UUID termId,
        String termCode,
        String code,
        String name
) {
    public static BatchResponse from(AcademicBatch batch) {
        return new BatchResponse(batch.getId(), batch.getTerm().getId(),
                batch.getTerm().getCode(), batch.getCode(), batch.getName());
    }
}
