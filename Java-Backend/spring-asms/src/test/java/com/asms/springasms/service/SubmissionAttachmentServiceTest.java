package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.entity.Assignment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.Submission;
import com.asms.springasms.entity.SubmissionAttachment;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.SubmissionAttachmentRepository;
import com.asms.springasms.repository.SubmissionRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class SubmissionAttachmentServiceTest {

    @Mock
    private SubmissionAttachmentRepository attachmentRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private TeacherCourseAllocationRepository allocationRepository;

    private SubmissionAttachmentService attachmentService() {
        return new SubmissionAttachmentService(attachmentRepository, submissionRepository,
                allocationRepository);
    }

    private Submission submission(UUID studentId, boolean closed) {
        Course course = new Course();
        course.setId(UUID.randomUUID());
        Assignment assignment = new Assignment();
        assignment.setId(UUID.randomUUID());
        assignment.setCourse(course);
        if (closed) {
            assignment.setSubmissionsClosedAt(Instant.now());
        }
        User student = new User();
        student.setId(studentId);
        Submission submission = new Submission();
        submission.setId(UUID.randomUUID());
        submission.setAssignment(assignment);
        submission.setStudent(student);
        return submission;
    }

    private MockMultipartFile pdf(String name, byte[] bytes, String contentType) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    @Test
    void upload_emptyFile_shouldThrowEmptyMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> attachmentService()
                .upload(UUID.randomUUID(), UUID.randomUUID(), pdf("a.pdf", new byte[0], "application/pdf")));
        assertEquals("Uploaded file is empty.", ex.getMessage());
    }

    @Test
    void upload_tooLarge_shouldThrowLimitMessage() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> attachmentService()
                .upload(UUID.randomUUID(), UUID.randomUUID(), pdf("a.pdf", big, "application/pdf")));
        assertEquals("File size exceeds the maximum allowed limit of 10 MB.", ex.getMessage());
    }

    @Test
    void upload_disallowedMime_shouldEchoOriginalCasing() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> attachmentService()
                .upload(UUID.randomUUID(), UUID.randomUUID(),
                        pdf("a.exe", new byte[]{1}, "Application/X-MSDOS-Program")));
        assertEquals("File type 'Application/X-MSDOS-Program' is not permitted.", ex.getMessage());
    }

    @Test
    void upload_unknownSubmission_shouldThrowResourceNotFound() {
        UUID submissionId = UUID.randomUUID();
        when(submissionRepository.findActiveWithAssignmentById(submissionId))
                .thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> attachmentService()
                .upload(submissionId, UUID.randomUUID(), pdf("a.pdf", new byte[]{1}, "application/pdf")));
        assertEquals("Resource Not Found", ex.getTitle());
        assertEquals("Submission with ID '" + submissionId + "' was not found.", ex.getMessage());
    }

    @Test
    void upload_otherStudentsSubmission_shouldThrowForbidden() {
        UUID ownerId = UUID.randomUUID();
        Submission submission = submission(ownerId, false);
        when(submissionRepository.findActiveWithAssignmentById(submission.getId()))
                .thenReturn(Optional.of(submission));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> attachmentService()
                .upload(submission.getId(), UUID.randomUUID(),
                        pdf("a.pdf", new byte[]{1}, "application/pdf")));
        assertEquals("You are not authorized to attach files to this submission.", ex.getMessage());
    }

    @Test
    void upload_closedAssignment_shouldThrowClosedMessage() {
        UUID studentId = UUID.randomUUID();
        Submission submission = submission(studentId, true);
        when(submissionRepository.findActiveWithAssignmentById(submission.getId()))
                .thenReturn(Optional.of(submission));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> attachmentService()
                .upload(submission.getId(), studentId, pdf("a.pdf", new byte[]{1}, "application/pdf")));
        assertEquals("Submissions for this assignment are closed.", ex.getMessage());
    }

    @Test
    void upload_success_shouldStripClientPath() {
        UUID studentId = UUID.randomUUID();
        Submission submission = submission(studentId, false);
        when(submissionRepository.findActiveWithAssignmentById(submission.getId()))
                .thenReturn(Optional.of(submission));
        when(attachmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var created = attachmentService().upload(submission.getId(), studentId,
                pdf("C:\\fakepath\\essay.pdf", new byte[]{1, 2, 3}, "application/pdf"));

        assertEquals("essay.pdf", created.originalFileName());
        assertEquals(3, created.byteSize());
    }

    @Test
    void download_studentReadingOthers_shouldThrowForbidden() {
        Submission submission = submission(UUID.randomUUID(), false);
        SubmissionAttachment attachment = new SubmissionAttachment();
        attachment.setId(UUID.randomUUID());
        attachment.setSubmission(submission);
        attachment.setContentType("application/pdf");
        attachment.setOriginalFileName("a.pdf");
        attachment.setFileData(new byte[]{1});
        when(attachmentRepository.findActiveByIdWithSubmissionAndAssignment(attachment.getId()))
                .thenReturn(Optional.of(attachment));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> attachmentService()
                .download(attachment.getId(), UUID.randomUUID(), UserRole.Student));
        assertEquals("You are not authorized to download this attachment.", ex.getMessage());
    }

    @Test
    void delete_closedAssignment_shouldThrowClosedMessage() {
        UUID studentId = UUID.randomUUID();
        Submission submission = submission(studentId, true);
        SubmissionAttachment attachment = new SubmissionAttachment();
        attachment.setId(UUID.randomUUID());
        attachment.setSubmission(submission);
        when(attachmentRepository.findActiveByIdWithSubmissionAndAssignment(attachment.getId()))
                .thenReturn(Optional.of(attachment));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> attachmentService()
                .delete(attachment.getId(), studentId));
        assertEquals("Submissions for this assignment are closed.", ex.getMessage());
        verify(attachmentRepository, never()).delete(any());
    }
}
