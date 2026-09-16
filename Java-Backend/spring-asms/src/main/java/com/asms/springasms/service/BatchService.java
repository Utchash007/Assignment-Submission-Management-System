package com.asms.springasms.service;

import com.asms.springasms.dto.batch.AssignStudentRequest;
import com.asms.springasms.dto.batch.BatchResponse;
import com.asms.springasms.dto.batch.BatchStudentResponse;
import com.asms.springasms.dto.batch.CreateBatchRequest;
import com.asms.springasms.dto.batch.SetBatchEnrollmentStatusRequest;
import com.asms.springasms.dto.batch.UpdateBatchRequest;
import com.asms.springasms.entity.AcademicBatch;
import com.asms.springasms.entity.AcademicTerm;
import com.asms.springasms.entity.BatchEnrollment;
import com.asms.springasms.entity.User;
import com.asms.springasms.enums.EnrollmentStatus;
import com.asms.springasms.enums.UserRole;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AcademicBatchRepository;
import com.asms.springasms.repository.AcademicTermRepository;
import com.asms.springasms.repository.BatchEnrollmentRepository;
import com.asms.springasms.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final AcademicBatchRepository batchRepository;
    private final AcademicTermRepository termRepository;
    private final BatchEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BatchResponse> getAll() {
        return batchRepository.findAllWithTermOrderByCodeAsc().stream()
                .map(BatchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BatchResponse getById(UUID id) {
        return batchRepository.findWithTermById(id)
                .map(BatchResponse::from)
                .orElseThrow(() -> new NotFoundException("Batch Not Found",
                        "Batch with ID '" + id + "' was not found."));
    }

    @Transactional
    public BatchResponse create(CreateBatchRequest request) {
        AcademicTerm term = termRepository.findById(request.termId())
                .orElseThrow(() -> new IllegalStateException(
                        "Academic term with ID '" + request.termId() + "' does not exist."));
        AcademicBatch batch = new AcademicBatch();
        batch.setTerm(term);
        batch.setCode(request.code().trim());
        batch.setName(request.name().trim());
        return BatchResponse.from(batchRepository.save(batch));
    }

    @Transactional
    public BatchResponse update(UUID id, UpdateBatchRequest request) {
        AcademicBatch batch = batchRepository.findWithTermById(id)
                .orElseThrow(() -> new NotFoundException("Batch Not Found",
                        "Batch with ID '" + id + "' was not found."));
        batch.setCode(request.code().trim());
        batch.setName(request.name().trim());
        return BatchResponse.from(batch);
    }

    @Transactional
    public void delete(UUID id) {
        AcademicBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Batch Not Found",
                        "Batch with ID '" + id + "' was not found."));
        if (enrollmentRepository.existsByBatchId(id)) {
            throw new IllegalStateException("Cannot delete batch because student enrollments exist.");
        }
        batchRepository.delete(batch);
    }

    @Transactional(readOnly = true)
    public List<BatchStudentResponse> getBatchStudents(UUID batchId) {
        return enrollmentRepository.findByBatchIdWithStudentOrderByStudentFullName(batchId).stream()
                .map(BatchStudentResponse::from).toList();
    }

    @Transactional
    public BatchStudentResponse assignStudent(UUID batchId, AssignStudentRequest request) {
        AcademicBatch batch = batchRepository.findWithTermById(batchId)
                .orElseThrow(() -> new IllegalStateException(
                        "Batch with ID '" + batchId + "' was not found."));
        User student = userRepository.findById(request.studentId()).orElse(null);
        if (student == null || student.getRole() != UserRole.Student) {
            throw new IllegalStateException("Assigned user must exist and have the Student role.");
        }
        if (enrollmentRepository.existsByBatchIdAndStudentId(batchId, request.studentId())) {
            throw new IllegalStateException("Student is already assigned to this batch.");
        }
        if (enrollmentRepository.existsActiveInTerm(request.studentId(), batch.getTerm().getId())) {
            throw new IllegalStateException(
                    "Student already has an active batch enrollment in this academic term.");
        }
        BatchEnrollment enrollment = new BatchEnrollment();
        enrollment.setBatch(batch);
        enrollment.setStudent(student);
        enrollment.setStatus(EnrollmentStatus.Active);
        return BatchStudentResponse.from(enrollmentRepository.save(enrollment));
    }

    @Transactional
    public void setEnrollmentStatus(UUID enrollmentId, SetBatchEnrollmentStatusRequest request) {
        BatchEnrollment enrollment = enrollmentRepository.findWithBatchById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Enrollment Not Found",
                        "Batch enrollment with ID '" + enrollmentId + "' was not found."));
        if (request.status() == EnrollmentStatus.Active && enrollment.getStatus() != EnrollmentStatus.Active
                && enrollmentRepository.existsActiveInTermExcluding(
                        enrollment.getStudent().getId(),
                        enrollment.getBatch().getTerm().getId(), enrollmentId)) {
            throw new IllegalStateException(
                    "Student already has an active batch enrollment in this academic term.");
        }
        enrollment.setStatus(request.status());
    }
}
