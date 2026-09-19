package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.enrollment.CourseStudentResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.service.CourseEnrollmentService;
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
class CourseEnrollmentsControllerTest {

    @Mock
    private CourseEnrollmentService enrollmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new CourseEnrollmentsController(enrollmentService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    @Test
    void enroll_shouldReturn201WithRoster() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(enrollmentService.enrollStudents(any())).thenReturn(List.of(
                new CourseStudentResponse(UUID.randomUUID(), UUID.randomUUID(), "Demo Student",
                        "student@onnorokom.com", "S-1001", "BATCH-2026-A", "Active")));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/course-enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + courseId + "\",\"batchEnrollmentIds\":[\""
                                + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "api/course-enrollments/courses/" + courseId + "/students"))
                .andExpect(jsonPath("$[0].batchCode").value("BATCH-2026-A"));
    }

    @Test
    void enroll_emptyIds_shouldReturn400Validation() throws Exception {
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/course-enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":\"" + UUID.randomUUID()
                                + "\",\"batchEnrollmentIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.BatchEnrollmentIds").exists());
    }

    @Test
    void getCourseStudents_shouldReturn200() throws Exception {
        UUID courseId = UUID.randomUUID();
        when(enrollmentService.getCourseStudents(courseId)).thenReturn(List.of());
        runAs(UserRole.Teacher);

        mockMvc.perform(get("/api/course-enrollments/courses/" + courseId + "/students"))
                .andExpect(status().isOk());
    }

    @Test
    void setStatus_shouldReturn204() throws Exception {
        runAs(UserRole.Admin);

        mockMvc.perform(patch("/api/course-enrollments/" + UUID.randomUUID() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isNoContent());
    }
}
