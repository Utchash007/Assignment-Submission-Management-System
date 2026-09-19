package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.allocation.CourseTeacherResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.service.TeacherCourseAllocationService;
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
class TeacherCourseAllocationsControllerTest {

    @Mock
    private TeacherCourseAllocationService allocationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new TeacherCourseAllocationsController(allocationService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    @Test
    void allocate_shouldReturn201() throws Exception {
        UUID allocationId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(allocationService.allocate(any())).thenReturn(
                new CourseTeacherResponse(allocationId, teacherId, "Demo Teacher",
                        "teacher@onnorokom.com", "Active"));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/teacher-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":\"" + teacherId + "\",\"courseId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teacherEmail").value("teacher@onnorokom.com"));
    }

    @Test
    void allocate_duplicate_shouldReturn400() throws Exception {
        when(allocationService.allocate(any()))
                .thenThrow(new IllegalStateException("Teacher is already allocated to this course."));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/teacher-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":\"" + UUID.randomUUID() + "\",\"courseId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Teacher is already allocated to this course."));
    }

    @Test
    void getTeacherCourses_shouldReturn200() throws Exception {
        UUID teacherId = UUID.randomUUID();
        when(allocationService.getTeacherCourses(teacherId)).thenReturn(List.of());
        runAs(UserRole.Teacher, teacherId);

        mockMvc.perform(get("/api/teacher-allocations/teachers/" + teacherId + "/courses"))
                .andExpect(status().isOk());
    }
}
