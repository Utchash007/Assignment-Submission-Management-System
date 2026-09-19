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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.asms.springasms.dto.user.UserResponse;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.service.UserService;
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
class UsersControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = mockMvc(new UsersController(userService));
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    private UserResponse student(UUID id) {
        return new UserResponse(id, "Demo Student", "student@onnorokom.com",
                "S-1001", "Student", true);
    }

    @Test
    void list_shouldReturnUsers() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUsers(null)).thenReturn(List.of(student(id)));
        runAs(UserRole.Admin);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student@onnorokom.com"))
                .andExpect(jsonPath("$[0].role").value("Student"));
    }

    @Test
    void create_shouldReturn201WithLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.createUser(any())).thenReturn(student(id));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Demo Student\",\"email\":\"student@onnorokom.com\","
                                + "\"password\":\"Student@123\",\"role\":\"Student\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void create_duplicateEmail_shouldReturn400BadRequest() throws Exception {
        when(userService.createUser(any())).thenThrow(
                new IllegalStateException("A user with email 'x@y.z' already exists."));
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"D\",\"email\":\"x@y.z\","
                                + "\"password\":\"secret1\",\"role\":\"Student\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("A user with email 'x@y.z' already exists."));
    }

    @Test
    void create_invalidEmail_shouldReturn400ValidationShape() throws Exception {
        runAs(UserRole.Admin);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"D\",\"email\":\"not-an-email\","
                                + "\"password\":\"secret1\",\"role\":\"Student\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.Email").exists());
    }

    @Test
    void getById_unknown_shouldReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.getUserById(id)).thenThrow(new NotFoundException("User Not Found",
                "User with ID '" + id + "' was not found."));
        runAs(UserRole.Admin);

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User Not Found"));
    }

    @Test
    void setActiveStatus_shouldReturn204() throws Exception {
        runAs(UserRole.Admin);

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID() + "/active-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.updateUser(eq(id), any())).thenReturn(student(id));
        runAs(UserRole.Admin);

        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Demo Student\",\"email\":\"student@onnorokom.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roll").value("S-1001"));
    }
}
