package com.asms.springasms.dto.attachment;

import com.asms.springasms.entity.SubmissionAttachment;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String originalFileName,
        String contentType,
        long byteSize
) {
    public static AttachmentResponse from(SubmissionAttachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalFileName(),
                a.getContentType(), a.getByteSize());
    }
}
