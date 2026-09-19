package com.asms.springasms.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Converts service-thrown exceptions to the exact RFC 7807 shapes the .NET backend
 * produces. Bodies are plain maps (not {@code ProblemDetail}) because Spring's
 * return-value handling unconditionally stamps {@code instance} onto
 * {@code ProblemDetail} responses, which the .NET contract omits.
 * Key order mirrors .NET: {title, status, detail} for app errors,
 * {type, title, status, errors, traceId} for validation failures.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String PROBLEM_JSON_UTF8 = "application/problem+json; charset=utf-8";
    private static final String VALIDATION_TYPE = "https://tools.ietf.org/html/rfc9110#section-15.5.1";

    private final Environment environment;

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<Map<String, Object>> handleInvalidArgument(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Upload Too Large", "File exceeds the 10MB limit.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler({
            org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class
    })
    ResponseEntity<Void> handleAccessDenied(Exception ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ResponseEntity<Void> handleSpringAuthentication(
            org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // Mirrors .NET's {id:guid} route constraint: non-UUID ids miss the route → 404 empty.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            String pascalField = field.isEmpty()
                    ? field
                    : Character.toUpperCase(field.charAt(0)) + field.substring(1);
            errors.computeIfAbsent(pascalField, k -> new ArrayList<>()).add(fieldError.getDefaultMessage());
        }
        // Stable sort: "is required." messages first, mirroring ASP.NET attribute order.
        for (List<String> messages : errors.values()) {
            messages.sort(java.util.Comparator.comparingInt(GlobalExceptionHandler::validationMessageOrder));
        }
        return validationProblem(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        return validationProblem(Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("An unhandled exception occurred during request processing.", ex);
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev", "development", "default"));
        String detail = isDev ? ex.getMessage() : "An unexpected error occurred.";
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail);
    }

    private ResponseEntity<Map<String, Object>> problem(HttpStatus status, String title, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail);
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON_UTF8)
                .body(body);
    }

    private ResponseEntity<Map<String, Object>> validationProblem(Map<String, List<String>> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", VALIDATION_TYPE);
        body.put("title", "One or more validation errors occurred.");
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errors", errors);
        body.put("traceId", UUID.randomUUID().toString());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, PROBLEM_JSON_UTF8)
                .body(body);
    }

    private static int validationMessageOrder(String message) {
        if (message != null && message.endsWith("is required.")) {
            return 0;
        }
        return 1;
    }
}
