package com.asms.springasms.controller;

import com.asms.springasms.dto.submission.ReviewSubmissionRequest;
import com.asms.springasms.dto.submission.SubmissionResponse;
import com.asms.springasms.dto.submission.UpsertSubmissionRequest;
import com.asms.springasms.security.CurrentUser;
import com.asms.springasms.service.SubmissionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionsController {

    private final SubmissionService submissionService;

    @GetMapping("/mine")
    @PreAuthorize("hasRole('Student')")
    public ResponseEntity<List<SubmissionResponse>> getMine(
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(submissionService.getMine(currentUser.id()));
    }

    @GetMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ResponseEntity<List<SubmissionResponse>> getForAssignment(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(submissionService.getForAssignment(
                assignmentId, currentUser.id(), currentUser.role()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getById(@PathVariable UUID id,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(submissionService.getById(id, currentUser.id(), currentUser.role()));
    }

    @PostMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasRole('Student')")
    public ResponseEntity<SubmissionResponse> submitOrUpdate(
            @PathVariable UUID assignmentId,
            @RequestBody(required = false) UpsertSubmissionRequest request,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(submissionService.upsert(assignmentId, currentUser.id(),
                request == null ? new UpsertSubmissionRequest(null) : request));
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('Teacher')")
    public ResponseEntity<SubmissionResponse> review(@PathVariable UUID id,
                                                     @Valid @RequestBody ReviewSubmissionRequest request,
                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(submissionService.review(id, currentUser.id(), request));
    }
}
