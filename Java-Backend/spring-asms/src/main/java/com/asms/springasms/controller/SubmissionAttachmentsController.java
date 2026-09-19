package com.asms.springasms.controller;

import com.asms.springasms.dto.attachment.AttachmentResponse;
import com.asms.springasms.security.CurrentUser;
import com.asms.springasms.service.SubmissionAttachmentService;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/submission-attachments")
@RequiredArgsConstructor
public class SubmissionAttachmentsController {

    private final SubmissionAttachmentService attachmentService;

    @PostMapping("/submissions/{submissionId}")
    @PreAuthorize("hasRole('Student')")
    public ResponseEntity<AttachmentResponse> upload(@PathVariable UUID submissionId,
                                                     @RequestParam("file") MultipartFile file,
                                                     @AuthenticationPrincipal CurrentUser currentUser) {
        AttachmentResponse created = attachmentService.upload(submissionId, currentUser.id(), file);
        return ResponseEntity.created(URI.create("api/submission-attachments/" + created.id()))
                .body(created);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable UUID attachmentId,
                                             @AuthenticationPrincipal CurrentUser currentUser) {
        SubmissionAttachmentService.Download download = attachmentService.download(
                attachmentId, currentUser.id(), currentUser.role());
        Resource resource = new ByteArrayResource(download.data());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.data().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition
                        .attachment().filename(download.fileName()).build().toString())
                .body(resource);
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasRole('Student')")
    public ResponseEntity<Void> delete(@PathVariable UUID attachmentId,
                                       @AuthenticationPrincipal CurrentUser currentUser) {
        attachmentService.delete(attachmentId, currentUser.id());
        return ResponseEntity.noContent().build();
    }
}
