package com.asms.springasms.service;

import com.asms.springasms.dto.submission.ReviewSubmissionRequest;
import com.asms.springasms.dto.submission.SubmissionResponse;
import com.asms.springasms.dto.submission.UpsertSubmissionRequest;
import com.asms.springasms.entity.Assignment;
import com.asms.springasms.entity.Submission;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.AssignmentStatus;
import com.asms.springasms.enums.SubmissionStatus;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.SubmissionRepository;
import com.asms.springasms.repository.TeacherCourseAllocationRepository;
import com.asms.springasms.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final TeacherCourseAllocationRepository allocationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getMine(UUID studentId) {
        return submissionRepository.findActiveByStudentIdWithDetails(studentId).stream()
                .map(SubmissionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getForAssignment(UUID assignmentId, UUID userId, UserRole role) {
        Assignment assignment = assignmentRepository.findActiveWithCourseById(assignmentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Assignment with ID '" + assignmentId + "' was not found."));
        if (role == UserRole.Teacher) {
            if (!allocationRepository.existsByTeacherIdAndCourseIdAndStatus(userId,
                    assignment.getCourse().getId(), TeacherCourseAllocationStatus.Active)) {
                throw new ForbiddenException("You are not allocated to the course for this assignment.");
            }
        } else if (role != UserRole.Admin) {
            throw new ForbiddenException("You are not authorized to view all submissions for this assignment.");
        }
        return submissionRepository.findActiveByAssignmentIdWithDetails(assignmentId).stream()
                .map(SubmissionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getById(UUID id, UUID userId, UserRole role) {
        Submission submission = submissionRepository.findActiveByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Submission Not Found",
                        "Submission with ID '" + id + "' was not found or is not accessible."));
        if (role == UserRole.Student) {
            if (!submission.getStudent().getId().equals(userId)) {
                throw new ForbiddenException("You are not authorized to access this submission.");
            }
        } else if (role == UserRole.Teacher) {
            if (!allocationRepository.existsByTeacherIdAndCourseIdAndStatus(userId,
                    submission.getAssignment().getCourse().getId(),
                    TeacherCourseAllocationStatus.Active)) {
                throw new ForbiddenException("You are not allocated to the course for this submission.");
            }
        }
        return SubmissionResponse.from(submission);
    }

    @Transactional
    public SubmissionResponse upsert(UUID assignmentId, UUID studentId, UpsertSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findActiveWithCourseById(assignmentId)
                .orElseThrow(() -> new IllegalStateException("Assignment is not available for submissions."));
        if (assignment.getStatus() != AssignmentStatus.Published) {
            throw new IllegalStateException("Assignment is not available for submissions.");
        }
        if (!enrollmentRepository.existsActiveEnrollment(assignment.getCourse().getId(), studentId)) {
            throw new ForbiddenException("You are not enrolled in the course for this assignment.");
        }
        if (assignment.getSubmissionsClosedAt() != null) {
            throw new IllegalStateException("Submissions for this assignment have been closed by the teacher.");
        }
        boolean isLate = Instant.now().isAfter(assignment.getDeadlineAt());
        Submission existing = submissionRepository
                .findActiveByAssignmentIdAndStudentIdWithAttachments(assignmentId, studentId)
                .orElse(null);
        if (existing != null) {
            if (!assignment.isAllowResubmission()) {
                throw new IllegalStateException("Resubmission is not permitted for this assignment.");
            }
            existing.setAnswerText(request.answerText());
            existing.setSubmittedAt(Instant.now());
            existing.setStatus(isLate ? SubmissionStatus.Late : SubmissionStatus.Submitted);
            // Parity quirk: .NET resubmit path keeps EvaluatedByUserId but returns EvaluatedByName=null
            SubmissionResponse base = SubmissionResponse.from(existing);
            return new SubmissionResponse(base.id(), base.assignmentId(), base.assignmentTitle(),
                    base.studentId(), base.studentName(), base.studentRoll(), base.answerText(),
                    base.status(), base.submittedAt(), base.marks(), base.feedback(),
                    base.evaluatedByUserId(), null, base.attachments());
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new NotFoundException("Submission Not Found",
                        "Submission with ID '" + assignmentId + "' was not found or is not accessible."));
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setAnswerText(request.answerText());
        submission.setSubmittedAt(Instant.now());
        submission.setStatus(isLate ? SubmissionStatus.Late : SubmissionStatus.Submitted);
        return SubmissionResponse.from(submissionRepository.save(submission));
    }

    @Transactional
    public SubmissionResponse review(UUID id, UUID teacherId, ReviewSubmissionRequest request) {
        Submission submission = submissionRepository.findActiveByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Submission Not Found",
                        "Submission with ID '" + id + "' was not found."));
        if (!allocationRepository.existsByTeacherIdAndCourseIdAndStatus(teacherId,
                submission.getAssignment().getCourse().getId(),
                TeacherCourseAllocationStatus.Active)) {
            throw new ForbiddenException("You are not allocated to the course for this submission.");
        }
        if (request.marks().compareTo(java.math.BigDecimal.ZERO) < 0
                || request.marks().compareTo(submission.getAssignment().getMaximumMarks()) > 0) {
            throw new IllegalStateException("Marks must be between 0 and "
                    + submission.getAssignment().getMaximumMarks() + ".");
        }
        User teacher = userRepository.findById(teacherId).orElse(null);
        submission.setMarks(request.marks());
        submission.setFeedback(request.feedback());
        submission.setStatus(request.status());
        submission.setEvaluatedBy(teacher);
        return SubmissionResponse.from(submission);
    }
}
