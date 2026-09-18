package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.attachment.AttachmentResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.service.SubmissionAttachmentService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class SubmissionAttachmentsControllerTest {

    @Mock
    private SubmissionAttachmentService attachmentService;

    private MockMvc mockMvc;
    private final UUID studentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new SubmissionAttachmentsController(attachmentService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    @Test
    void upload_shouldReturn201() throws Exception {
        UUID submissionId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        when(attachmentService.upload(org.mockito.ArgumentMatchers.eq(submissionId),
                        org.mockito.ArgumentMatchers.eq(studentId), any()))
                .thenReturn(new AttachmentResponse(attachmentId, "essay.pdf",
                        "application/pdf", 3));
        runAs(UserRole.Student, studentId);

        MockMultipartFile file =
                new MockMultipartFile("file", "essay.pdf", "application/pdf", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/submission-attachments/submissions/" + submissionId)
                        .file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("essay.pdf"))
                .andExpect(jsonPath("$.byteSize").value(3));
    }

    @Test
    void upload_emptyFile_shouldReturn400() throws Exception {
        UUID submissionId = UUID.randomUUID();
        when(attachmentService.upload(org.mockito.ArgumentMatchers.eq(submissionId),
                        org.mockito.ArgumentMatchers.eq(studentId), any()))
                .thenThrow(new IllegalStateException("Uploaded file is empty."));
        runAs(UserRole.Student, studentId);

        MockMultipartFile file =
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/submission-attachments/submissions/" + submissionId)
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void download_shouldReturnBytesWithContentType() throws Exception {
        UUID attachmentId = UUID.randomUUID();
        byte[] data = {1, 2, 3, 4};
        when(attachmentService.download(
                        org.mockito.ArgumentMatchers.eq(attachmentId),
                        org.mockito.ArgumentMatchers.eq(studentId),
                        org.mockito.ArgumentMatchers.eq(UserRole.Student)))
                .thenReturn(new SubmissionAttachmentService.Download(data, "application/pdf",
                        "essay.pdf"));
        runAs(UserRole.Student, studentId);

        mockMvc.perform(get("/api/submission-attachments/" + attachmentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(data));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        runAs(UserRole.Student, studentId);

        mockMvc.perform(delete("/api/submission-attachments/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
