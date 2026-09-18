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

import com.asms.springasms.dto.assignment.AssignmentResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.service.AssignmentService;
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
class AssignmentsControllerTest {

    @Mock
    private AssignmentService assignmentService;

    private MockMvc mockMvc;
    private final UUID teacherId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new AssignmentsController(assignmentService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    private AssignmentResponse response(UUID id) {
        return new AssignmentResponse(id, UUID.randomUUID(), "Introduction to Programming",
                "CSE101", "Assignment 1", null, Instant.parse("2026-10-01T00:00:00Z"),
                new BigDecimal("100"), "Published", true, null, teacherId, "Demo Teacher");
    }

    @Test
    void list_shouldReturnAssignments() throws Exception {
        when(assignmentService.getAssignments(eq(teacherId), eq(UserRole.Teacher), eq(null)))
                .thenReturn(List.of(response(UUID.randomUUID())));
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(get("/api/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseCode").value("CSE101"))
                .andExpect(jsonPath("$[0].status").value("Published"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        UUID id = UUID.randomUUID();
        when(assignmentService.create(eq(teacherId), any())).thenReturn(response(id));
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + UUID.randomUUID() + "\",\"title\":\"T\","
                                + "\"deadlineAt\":\"2026-10-01T00:00:00Z\",\"maximumMarks\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void create_unallocated_shouldReturn403Forbidden() throws Exception {
        when(assignmentService.create(eq(teacherId), any())).thenThrow(
                new ForbiddenException("Teacher does not have an active allocation to this course."));
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + UUID.randomUUID() + "\",\"title\":\"T\","
                                + "\"deadlineAt\":\"2026-10-01T00:00:00Z\",\"maximumMarks\":100}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void create_missingTitle_shouldReturn400() throws Exception {
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(post("/api/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + UUID.randomUUID() + "\","
                                + "\"deadlineAt\":\"2026-10-01T00:00:00Z\",\"maximumMarks\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.Title").exists());
    }

    @Test
    void publish_shouldReturn204() throws Exception {
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(patch("/api/assignments/" + UUID.randomUUID() + "/publish"))
                .andExpect(status().isNoContent());
    }
}
