package com.asms.springasms.controller;

import com.asms.springasms.dto.batch.AssignStudentRequest;
import com.asms.springasms.dto.batch.BatchResponse;
import com.asms.springasms.dto.batch.BatchStudentResponse;
import com.asms.springasms.dto.batch.CreateBatchRequest;
import com.asms.springasms.dto.batch.SetBatchEnrollmentStatusRequest;
import com.asms.springasms.dto.batch.UpdateBatchRequest;
import com.asms.springasms.service.BatchService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchesController {

    private final BatchService batchService;

    @GetMapping
    public ResponseEntity<List<BatchResponse>> getAll() {
        return ResponseEntity.ok(batchService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<BatchResponse> create(@Valid @RequestBody CreateBatchRequest request) {
        BatchResponse created = batchService.create(request);
        return ResponseEntity.created(URI.create("/api/batches/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<BatchResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateBatchRequest request) {
        return ResponseEntity.ok(batchService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        batchService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('Admin','Teacher')")
    public ResponseEntity<List<BatchStudentResponse>> getStudents(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getBatchStudents(id));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<BatchStudentResponse> assignStudent(@PathVariable UUID id,
                                                              @Valid @RequestBody AssignStudentRequest request) {
        BatchStudentResponse created = batchService.assignStudent(id, request);
        return ResponseEntity.created(URI.create("api/batches/" + id + "/students/" + created.enrollmentId()))
                .body(created);
    }

    @PatchMapping("/{id}/enrollments/{enrollmentId}/status")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> setEnrollmentStatus(@PathVariable UUID id,
                                                    @PathVariable UUID enrollmentId,
                                                    @Valid @RequestBody SetBatchEnrollmentStatusRequest request) {
        batchService.setEnrollmentStatus(enrollmentId, request);
        return ResponseEntity.noContent().build();
    }
}
