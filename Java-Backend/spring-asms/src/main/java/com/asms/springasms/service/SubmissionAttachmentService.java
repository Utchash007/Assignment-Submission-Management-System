package com.asms.springasms.service;

import com.asms.springasms.dto.attachment.AttachmentResponse;
import com.asms.springasms.entity.Submission;
import com.asms.springasms.entity.SubmissionAttachment;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.SubmissionAttachmentRepository;
import com.asms.springasms.repository.SubmissionRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SubmissionAttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "application/zip",
            "application/x-zip-compressed");

    private final SubmissionAttachmentRepository attachmentRepository;
    private final SubmissionRepository submissionRepository;
    private final TeacherCourseAllocationRepository allocationRepository;

    @Transactional
    public AttachmentResponse upload(UUID submissionId, UUID studentId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new IllegalStateException("Uploaded file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalStateException("File size exceeds the maximum allowed limit of "
                    + (MAX_FILE_SIZE_BYTES / 1024 / 1024) + " MB.");
        }
        String contentType = file.getContentType();
        if (!ALLOWED_CONTENT_TYPES.isEmpty()
                && (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))) {
            throw new IllegalStateException("File type '" + contentType + "' is not permitted.");
        }
        Submission submission = submissionRepository.findActiveWithAssignmentById(submissionId)
                .orElseThrow(() -> new NotFoundException("Resource Not Found",
                        "Submission with ID '" + submissionId + "' was not found."));
        if (!submission.getStudent().getId().equals(studentId)) {
            throw new ForbiddenException("You are not authorized to attach files to this submission.");
        }
        if (submission.getAssignment().getSubmissionsClosedAt() != null) {
            throw new IllegalStateException("Submissions for this assignment are closed.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Uploaded file is empty.");
        }
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
            if (slash >= 0) {
                fileName = fileName.substring(slash + 1);
            }
        }
        SubmissionAttachment attachment = new SubmissionAttachment();
        attachment.setSubmission(submission);
        attachment.setOriginalFileName(fileName);
        attachment.setContentType(contentType);
        attachment.setByteSize(file.getSize());
        attachment.setFileData(data);
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public Download download(UUID attachmentId, UUID userId, UserRole role) {
        SubmissionAttachment attachment = attachmentRepository
                .findActiveByIdWithSubmissionAndAssignment(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment Not Found",
                        "Attachment with ID '" + attachmentId + "' was not found or is not accessible."));
        Submission submission = attachment.getSubmission();
        if (role == UserRole.Student) {
            if (!submission.getStudent().getId().equals(userId)) {
                throw new ForbiddenException("You are not authorized to download this attachment.");
            }
        } else if (role == UserRole.Teacher) {
            if (!allocationRepository.existsByTeacherIdAndCourseIdAndStatus(userId,
                    submission.getAssignment().getCourse().getId(),
                    TeacherCourseAllocationStatus.Active)) {
                throw new ForbiddenException("You are not allocated to the course for this attachment.");
            }
        }
        return new Download(attachment.getFileData(), attachment.getContentType(),
                attachment.getOriginalFileName());
    }

    @Transactional
    public void delete(UUID attachmentId, UUID studentId) {
        SubmissionAttachment attachment = attachmentRepository
                .findActiveByIdWithSubmissionAndAssignment(attachmentId)
                .orElseThrow(() -> new NotFoundException("Attachment Not Found",
                        "Attachment with ID '" + attachmentId + "' was not found."));
        Submission submission = attachment.getSubmission();
        if (!submission.getStudent().getId().equals(studentId)) {
            throw new ForbiddenException("You are not authorized to delete this attachment.");
        }
        if (submission.getAssignment().getSubmissionsClosedAt() != null) {
            throw new IllegalStateException("Submissions for this assignment are closed.");
        }
        attachmentRepository.delete(attachment);
    }

    public record Download(byte[] data, String contentType, String fileName) {
    }
}
