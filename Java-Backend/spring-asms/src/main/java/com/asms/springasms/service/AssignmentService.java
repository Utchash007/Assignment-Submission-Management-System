package com.asms.springasms.service;

import com.asms.springasms.dto.assignment.AssignmentResponse;
import com.asms.springasms.dto.assignment.CreateAssignmentRequest;
import com.asms.springasms.dto.assignment.UpdateAssignmentRequest;
import com.asms.springasms.entity.Assignment;
import com.asms.springasms.entity.Course;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.AssignmentStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.ForbiddenException;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AssignmentRepository;
import com.asms.springasms.repository.CourseEnrollmentRepository;
import com.asms.springasms.repository.CourseRepository;
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
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final TeacherCourseAllocationRepository allocationRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignments(UUID userId, UserRole role, UUID courseIdFilter) {
        List<Assignment> assignments;
        if (role == UserRole.Teacher) {
            List<UUID> courseIds = allocationRepository.findActiveCourseIdsByTeacherId(userId);
            if (courseIds.isEmpty()) {
                return List.of();
            }
            assignments = courseIdFilter == null
                    ? assignmentRepository.findActiveByCourseIdInWithDetails(courseIds)
                    : courseIds.contains(courseIdFilter)
                            ? assignmentRepository.findActiveByCourseIdWithDetails(courseIdFilter)
                            : List.of();
        } else if (role == UserRole.Student) {
            List<UUID> courseIds = enrollmentRepository.findActiveCourseIdsByStudentId(userId);
            if (courseIds.isEmpty()) {
                return List.of();
            }
            if (courseIdFilter != null && !courseIds.contains(courseIdFilter)) {
                return List.of();
            }
            assignments = courseIdFilter == null
                    ? assignmentRepository.findActiveByStatusAndCourseIdInWithDetails(
                            AssignmentStatus.Published, courseIds)
                    : assignmentRepository.findActiveByStatusAndCourseIdInWithDetails(
                            AssignmentStatus.Published, List.of(courseIdFilter)).stream()
                            .filter(a -> courseIds.contains(a.getCourse().getId())).toList();
        } else {
            assignments = courseIdFilter == null
                    ? assignmentRepository.findAllActiveWithDetailsOrderByDeadlineAtDesc()
                    : assignmentRepository.findActiveByCourseIdWithDetails(courseIdFilter);
        }
        return assignments.stream().map(AssignmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getById(UUID id, UUID userId, UserRole role) {
        Assignment assignment = assignmentRepository.findActiveWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Assignment Not Found",
                        "Assignment with ID '" + id + "' was not found or is not accessible."));
        if (role == UserRole.Teacher) {
            if (!allocationRepository.existsByTeacherIdAndCourseIdAndStatus(userId,
                    assignment.getCourse().getId(),
                    com.asms.springasms.enums.TeacherCourseAllocationStatus.Active)) {
                throw new ForbiddenException("You are not allocated to the course for this assignment.");
            }
        } else if (role == UserRole.Student) {
            if (assignment.getStatus() != AssignmentStatus.Published) {
                throw new NotFoundException("Assignment Not Found",
                        "Assignment with ID '" + id + "' was not found or is not accessible.");
            }
            if (!enrollmentRepository.existsActiveEnrollment(
                    assignment.getCourse().getId(), userId)) {
                throw new ForbiddenException("You are not enrolled in the course for this assignment.");
            }
        }
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public AssignmentResponse create(UUID teacherId, CreateAssignmentRequest request) {
        if (!allocationRepository.existsByTeacherIdAndCourseIdAndStatus(teacherId, request.courseId(),
                com.asms.springasms.enums.TeacherCourseAllocationStatus.Active)) {
            throw new ForbiddenException("Teacher does not have an active allocation to this course.");
        }
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalStateException(
                        "Course with ID '" + request.courseId() + "' does not exist."));
        User teacher = userRepository.findById(teacherId).orElse(null);
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setCreatedBy(teacher);
        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        assignment.setDeadlineAt(request.deadlineAt());
        assignment.setMaximumMarks(request.maximumMarks());
        assignment.setStatus(AssignmentStatus.Draft);
        assignment.setAllowResubmission(request.allowResubmission());
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Transactional
    public AssignmentResponse update(UUID id, UUID teacherId, UpdateAssignmentRequest request) {
        Assignment assignment = assignmentRepository.findActiveWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Assignment Not Found",
                        "Assignment with ID '" + id + "' was not found."));
        if (!assignment.getCreatedBy().getId().equals(teacherId)) {
            throw new ForbiddenException("Only the teacher who created this assignment can modify it.");
        }
        if (assignment.getStatus() == AssignmentStatus.Published
                && submissionRepository.existsActiveByAssignmentId(id)) {
            throw new IllegalStateException(
                    "Cannot modify assignment details because student submissions already exist.");
        }
        assignment.setTitle(request.title().trim());
        assignment.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        assignment.setDeadlineAt(request.deadlineAt());
        assignment.setMaximumMarks(request.maximumMarks());
        assignment.setAllowResubmission(request.allowResubmission());
        return AssignmentResponse.from(assignment);
    }

    @Transactional
    public void publish(UUID id, UUID teacherId) {
        Assignment assignment = assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Assignment Not Found",
                        "Assignment with ID '" + id + "' was not found."));
        if (!assignment.getCreatedBy().getId().equals(teacherId)) {
            throw new ForbiddenException("Only the teacher who created this assignment can publish it.");
        }
        assignment.setStatus(AssignmentStatus.Published);
    }

    @Transactional
    public void closeSubmissions(UUID id, UUID teacherId) {
        Assignment assignment = assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Assignment Not Found",
                        "Assignment with ID '" + id + "' was not found."));
        if (!assignment.getCreatedBy().getId().equals(teacherId)) {
            throw new ForbiddenException("Only the teacher who created this assignment can close submissions.");
        }
        assignment.setSubmissionsClosedAt(Instant.now());
    }

    @Transactional
    public void delete(UUID id, UUID teacherId) {
        Assignment assignment = assignmentRepository.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Assignment Not Found",
                        "Assignment with ID '" + id + "' was not found."));
        if (!assignment.getCreatedBy().getId().equals(teacherId)) {
            throw new ForbiddenException("Only the teacher who created this assignment can delete it.");
        }
        assignment.setDeletedAt(Instant.now());
    }
}
