package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.course.CourseResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.service.CourseService;
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
class CoursesControllerTest {

    @Mock
    private CourseService courseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new CoursesController(courseService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    @Test
    void list_shouldReturnCourses() throws Exception {
        when(courseService.getAll()).thenReturn(List.of(
                new CourseResponse(UUID.randomUUID(), "CSE101", "Introduction to Programming", null)));
        runAs(UserRole.Student);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CSE101"));
    }

    @Test
    void delete_referencedCourse_shouldReturn400() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new IllegalStateException(
                        "Cannot delete course because teacher allocations, student enrollments, "
                                + "or assignments exist."))
                .when(courseService).delete(id);
        runAs(UserRole.Admin);

        mockMvc.perform(delete("/api/courses/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        UUID id = UUID.randomUUID();
        when(courseService.create(any()))
                .thenReturn(new CourseResponse(id, "CSE103", "Database Management Systems", null));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CSE103\",\"title\":\"Database Management Systems\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }
}
