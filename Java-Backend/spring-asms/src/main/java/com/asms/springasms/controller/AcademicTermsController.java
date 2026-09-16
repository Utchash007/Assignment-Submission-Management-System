package com.asms.springasms.controller;

import com.asms.springasms.dto.term.AcademicTermResponse;
import com.asms.springasms.dto.term.CreateAcademicTermRequest;
import com.asms.springasms.dto.term.UpdateAcademicTermRequest;
import com.asms.springasms.service.AcademicTermService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/academic-terms")
@RequiredArgsConstructor
public class AcademicTermsController {

    private final AcademicTermService termService;

    @GetMapping
    public ResponseEntity<List<AcademicTermResponse>> getAll() {
        return ResponseEntity.ok(termService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicTermResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(termService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<AcademicTermResponse> create(@Valid @RequestBody CreateAcademicTermRequest request) {
        AcademicTermResponse created = termService.create(request);
        return ResponseEntity.created(URI.create("/api/academic-terms/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<AcademicTermResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateAcademicTermRequest request) {
        return ResponseEntity.ok(termService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        termService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
