package com.asms.springasms.controller;

import static com.asms.springasms.controller.ControllerTestSupport.mockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new HealthController(userRepository));
    }

    @Test
    void health_databaseUp_shouldReportCounts() throws Exception {
        when(userRepository.count()).thenReturn(3L);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.users").value(3));
    }

    @Test
    void health_databaseDown_shouldStillReturn200WithDownFlag() throws Exception {
        when(userRepository.count()).thenThrow(new RuntimeException("connection refused"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("UP"))
                .andExpect(jsonPath("$.database").value("DOWN"));
    }
}
