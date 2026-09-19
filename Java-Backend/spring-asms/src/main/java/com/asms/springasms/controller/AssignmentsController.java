package com.asms.springasms.controller;

import com.asms.springasms.dto.assignment.AssignmentResponse;
import com.asms.springasms.dto.assignment.CreateAssignmentRequest;
import com.asms.springasms.dto.assignment.UpdateAssignmentRequest;
import com.asms.springasms.security.CurrentUser;
import com.asms.springasms.service.AssignmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentsController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAll(
            @RequestParam(required = false) UUID courseId,
            @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(assignmentService.getAssignments(
                currentUser.id(), currentUser.role(), courseId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> getById(@PathVariable UUID id,
                                                      @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(assignmentService.getById(id, currentUser.id(), currentUser.role()));
    }

    @PostMapping
    @PreAuthorize("hasRole('Teacher')")
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request,
                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        AssignmentResponse created = assignmentService.create(currentUser.id(), request);
        return ResponseEntity.created(URI.create("/api/assignments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Teacher')")
    public ResponseEntity<AssignmentResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateAssignmentRequest request,
                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(assignmentService.update(id, currentUser.id(), request));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('Teacher')")
    public ResponseEntity<Void> publish(@PathVariable UUID id,
                                        @AuthenticationPrincipal CurrentUser currentUser) {
        assignmentService.publish(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/close-submissions")
    @PreAuthorize("hasRole('Teacher')")
    public ResponseEntity<Void> closeSubmissions(@PathVariable UUID id,
                                                 @AuthenticationPrincipal CurrentUser currentUser) {
        assignmentService.closeSubmissions(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Teacher')")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal CurrentUser currentUser) {
        assignmentService.delete(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
