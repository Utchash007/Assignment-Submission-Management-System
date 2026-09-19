package com.asms.springasms;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.GlobalExceptionHandler;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        void throwNotFound() {
            throw new NotFoundException("User Not Found", "User with ID '00000000-0000-0000-0000-000000000001' was not found.");
        }

        @GetMapping("/forbidden")
        void throwForbidden() {
            throw new ForbiddenException("Forbidden", "Access is denied.");
        }

        @GetMapping("/unauthorized")
        void throwUnauthorized() {
            throw new UnauthorizedException("Unauthorized", "Invalid email or password.");
        }

        @GetMapping("/bad-request")
        void throwBadRequest() {
            throw new IllegalStateException("Operation not allowed.");
        }

        @GetMapping("/invalid-arg")
        void throwInvalidArg() {
            throw new IllegalArgumentException("Invalid date range.");
        }

        @GetMapping("/upload-too-large")
        void throwUploadTooLarge() {
            throw new MaxUploadSizeExceededException(10 * 1024 * 1024);
        }

        record TestValidationDto(
                @NotBlank(message = "The Email field is required.")
                @Email(message = "The Email field is not a valid e-mail address.")
                String email,

                @NotBlank(message = "The Password field is required.")
                String password
        ) {}

        @PostMapping("/validate")
        void validateBody(@Valid @RequestBody TestValidationDto dto) {
        }
    }

    @BeforeEach
    void setUp() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(env);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(handler)
                .build();
    }

    @Test
    void handleNotFound_shouldMatchProblemDetailContract() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.title", is("User Not Found")))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.detail", is("User with ID '00000000-0000-0000-0000-000000000001' was not found.")))
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @Test
    void handleUnauthorized_shouldMatchLoginBadPasswordContract() throws Exception {
        mockMvc.perform(get("/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.title", is("Unauthorized")))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.detail", is("Invalid email or password.")))
                .andExpect(jsonPath("$.type").doesNotExist());
    }

    @Test
    void handleForbidden_shouldMatchProblemDetailContract() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.title", is("Forbidden")))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.detail", is("Access is denied.")));
    }

    @Test
    void handleIllegalState_shouldReturn400BadRequest() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("Operation not allowed.")));
    }

    @Test
    void handleUploadTooLarge_shouldReturn400() throws Exception {
        mockMvc.perform(get("/test/upload-too-large"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Upload Too Large")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("File exceeds the 10MB limit.")));
    }

    @Test
    void handleValidation_shouldReturnRfc9110WithPascalCaseErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.type", is("https://tools.ietf.org/html/rfc9110#section-15.5.1")))
                .andExpect(jsonPath("$.title", is("One or more validation errors occurred.")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors.Email", hasItem("The Email field is required.")))
                .andExpect(jsonPath("$.errors.Password", hasItem("The Password field is required.")))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
