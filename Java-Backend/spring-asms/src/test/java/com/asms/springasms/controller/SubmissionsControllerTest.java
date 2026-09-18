package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.submission.SubmissionResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.service.SubmissionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class SubmissionsControllerTest {

    @Mock
    private SubmissionService submissionService;

    private MockMvc mockMvc;
    private final UUID studentId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new SubmissionsController(submissionService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    private SubmissionResponse response(UUID id, String status) {
        return new SubmissionResponse(id, UUID.randomUUID(), "Assignment 1", studentId,
                "Demo Student", "S-1001", "answer", status, Instant.parse("2026-09-20T00:00:00Z"),
                null, null, null, null, List.of());
    }

    @Test
    void mine_shouldReturnSubmissions() throws Exception {
        when(submissionService.getMine(studentId))
                .thenReturn(List.of(response(UUID.randomUUID(), "Submitted")));
        runAs(UserRole.Student, studentId);

        mockMvc.perform(get("/api/submissions/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentRoll").value("S-1001"))
                .andExpect(jsonPath("$[0].status").value("Submitted"));
    }

    @Test
    void submitOrUpdate_shouldReturn200() throws Exception {
        UUID assignmentId = UUID.randomUUID();
        when(submissionService.upsert(eq(assignmentId), eq(studentId), any()))
                .thenReturn(response(UUID.randomUUID(), "Submitted"));
        runAs(UserRole.Student, studentId);

        mockMvc.perform(post("/api/submissions/assignments/" + assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerText\":\"my answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerText").value("answer"));
    }

    @Test
    void review_outOfBounds_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        when(submissionService.review(eq(id), eq(teacherId), any())).thenThrow(
                new IllegalStateException("Marks must be between 0 and 100."));
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(patch("/api/submissions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":150,\"status\":\"Reviewed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Marks must be between 0 and 100."));
    }

    @Test
    void review_closed_shouldReturn400WithServiceMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(submissionService.review(eq(id), eq(teacherId), any())).thenThrow(
                new IllegalStateException(
                        "Submissions for this assignment have been closed by the teacher."));
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(patch("/api/submissions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":80,\"status\":\"Reviewed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void review_missingStatus_shouldReturn400() throws Exception {
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(patch("/api/submissions/" + UUID.randomUUID() + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marks\":80}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.Status").exists());
    }

    @Test
    void gradedResponse_shouldSerializeMarks() throws Exception {
        SubmissionResponse graded = new SubmissionResponse(UUID.randomUUID(), UUID.randomUUID(),
                "Assignment 1", studentId, "Demo Student", "S-1001", "answer", "Reviewed",
                Instant.parse("2026-09-20T00:00:00Z"), new BigDecimal("85"), "Well done",
                teacherId, "Demo Teacher", List.of());
        when(submissionService.getById(eq(graded.id()), eq(studentId), eq(UserRole.Student)))
                .thenReturn(graded);
        runAs(UserRole.Student, studentId);

        mockMvc.perform(get("/api/submissions/" + graded.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marks").value(85))
                .andExpect(jsonPath("$.evaluatedByName").value("Demo Teacher"));
    }
}
