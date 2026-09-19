package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.clearAuth;
import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static com.asms.springasms.controller.ControllerTestSupport.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.term.AcademicTermResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.service.AcademicTermService;
import java.time.LocalDate;
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
class AcademicTermsControllerTest {

    @Mock
    private AcademicTermService termService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new AcademicTermsController(termService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    private AcademicTermResponse term(UUID id) {
        return new AcademicTermResponse(id, "FALL2026",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31));
    }

    @Test
    void list_shouldReturnTerms() throws Exception {
        when(termService.getAll()).thenReturn(List.of(term(UUID.randomUUID())));
        runAs(UserRole.Student);

        mockMvc.perform(get("/api/academic-terms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FALL2026"))
                .andExpect(jsonPath("$[0].startsOn").value("2026-09-01"));
    }

    @Test
    void create_shouldReturn201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(termService.create(any())).thenReturn(term(id));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/academic-terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FALL2026\",\"startsOn\":\"2026-09-01\","
                                + "\"endsOn\":\"2026-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/academic-terms/" + id));
    }

    @Test
    void create_badDates_shouldReturn400WithServiceMessage() throws Exception {
        when(termService.create(any()))
                .thenThrow(new IllegalStateException("Term start date cannot be after end date."));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/academic-terms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"startsOn\":\"2026-12-31\",\"endsOn\":\"2026-09-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Term start date cannot be after end date."));
    }

    @Test
    void getById_unknown_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(termService.getById(id)).thenThrow(new NotFoundException("Academic Term Not Found",
                "Academic term with ID '" + id + "' was not found."));
        runAs(UserRole.Teacher);

        mockMvc.perform(get("/api/academic-terms/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Academic Term Not Found"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        runAs(UserRole.Admin);

        mockMvc.perform(delete("/api/academic-terms/" + UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(termService.update(eq(id), any())).thenReturn(term(id));
        runAs(UserRole.Admin);

        mockMvc.perform(put("/api/academic-terms/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FALL2026\",\"startsOn\":\"2026-09-01\","
                                + "\"endsOn\":\"2026-12-31\"}"))
                .andExpect(status().isOk());
    }
}
