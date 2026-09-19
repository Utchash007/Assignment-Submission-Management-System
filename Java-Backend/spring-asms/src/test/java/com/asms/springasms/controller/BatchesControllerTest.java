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

import com.asms.springasms.dto.batch.BatchResponse;
import com.asms.springasms.dto.batch.BatchStudentResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.service.BatchService;
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
class BatchesControllerTest {

    @Mock
    private BatchService batchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new BatchesController(batchService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    @Test
    void getStudents_shouldReturnRoster() throws Exception {
        UUID batchId = UUID.randomUUID();
        when(batchService.getBatchStudents(batchId)).thenReturn(List.of(
                new BatchStudentResponse(UUID.randomUUID(), UUID.randomUUID(), "Demo Student",
                        "student@onnorokom.com", "S-1001", "Active")));
        runAs(UserRole.Teacher);

        mockMvc.perform(get("/api/batches/" + batchId + "/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentRoll").value("S-1001"))
                .andExpect(jsonPath("$[0].status").value("Active"));
    }

    @Test
    void assignStudent_duplicate_shouldReturn400() throws Exception {
        UUID batchId = UUID.randomUUID();
        when(batchService.assignStudent(eq(batchId), any()))
                .thenThrow(new IllegalStateException("Student is already assigned to this batch."));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/batches/" + batchId + "/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Student is already assigned to this batch."));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        UUID batchId = UUID.randomUUID();
        UUID termId = UUID.randomUUID();
        when(batchService.create(any())).thenReturn(
                new BatchResponse(batchId, termId, "FALL2026", "BATCH-2026-A", "Batch 2026 Section A"));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"termId\":\"" + termId + "\",\"code\":\"BATCH-2026-A\","
                                + "\"name\":\"Batch 2026 Section A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.termCode").value("FALL2026"));
    }

    @Test
    void setEnrollmentStatus_shouldReturn204() throws Exception {
        runAs(UserRole.Admin);

        mockMvc.perform(patch("/api/batches/" + UUID.randomUUID() + "/enrollments/"
                                + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isNoContent());
    }
}
