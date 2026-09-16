package com.asms.springasms.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbidden(ForbiddenException ex) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, ex.getTitle(), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ProblemDetail handleInvalidArgument(Exception ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Argument", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Upload Too Large", "File exceeds the 10MB limit.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("https://tools.ietf.org/html/rfc9110#section-15.5.1"));
        pd.setTitle("One or more validation errors occurred.");

        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            String pascalField = field.isEmpty()
                    ? field
                    : Character.toUpperCase(field.charAt(0)) + field.substring(1);
            errors.computeIfAbsent(pascalField, k -> new ArrayList<>()).add(fieldError.getDefaultMessage());
        }

        pd.setProperty("errors", errors);
        pd.setProperty("traceId", UUID.randomUUID().toString());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleGeneric(Exception ex) {
        log.error("An unhandled exception occurred during request processing.", ex);
        boolean isDev = environment.acceptsProfiles(Profiles.of("dev", "development", "default"));
        String detail = isDev ? ex.getMessage() : "An unexpected error occurred.";
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail);
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(null);
        pd.setTitle(title);
        return pd;
    }
}
