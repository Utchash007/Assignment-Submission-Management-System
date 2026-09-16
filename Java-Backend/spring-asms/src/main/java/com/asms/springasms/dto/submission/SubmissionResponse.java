package com.asms.springasms.dto.submission;

import com.asms.springasms.entity.Submission;
import com.asms.springasms.entity.SubmissionAttachment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID assignmentId,
        String assignmentTitle,
        UUID studentId,
        String studentName,
        String studentRoll,
        String answerText,
        String status,
        Instant submittedAt,
        BigDecimal marks,
        String feedback,
        UUID evaluatedByUserId,
        String evaluatedByName,
        List<AttachmentInfo> attachments
) {
    public static SubmissionResponse from(Submission s) {
        List<AttachmentInfo> infos = s.getAttachments() == null ? List.of()
                : s.getAttachments().stream()
                        .map(a -> new AttachmentInfo(a.getId(), a.getOriginalFileName(),
                                a.getContentType(), a.getByteSize()))
                        .toList();
        return new SubmissionResponse(s.getId(), s.getAssignment().getId(),
                s.getAssignment().getTitle(), s.getStudent().getId(),
                s.getStudent().getFullName(), s.getStudent().getRoll(),
                s.getAnswerText(), s.getStatus().name(), s.getSubmittedAt(),
                s.getMarks(), s.getFeedback(),
                s.getEvaluatedBy() == null ? null : s.getEvaluatedBy().getId(),
                s.getEvaluatedBy() == null ? null : s.getEvaluatedBy().getFullName(),
                infos);
    }

    public record AttachmentInfo(
            UUID id,
            String originalFileName,
            String contentType,
            long byteSize
    ) {
    }
}
