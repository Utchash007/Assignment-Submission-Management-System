package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.batch.AssignStudentRequest;
import com.asms.springasms.dto.batch.CreateBatchRequest;
import com.asms.springasms.dto.batch.SetBatchEnrollmentStatusRequest;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private AcademicBatchRepository batchRepository;
    @Mock
    private AcademicTermRepository termRepository;
    @Mock
    private BatchEnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;

    private BatchService batchService() {
        return new BatchService(batchRepository, termRepository, enrollmentRepository, userRepository);
    }

    private AcademicBatch batchWithTerm() {
        AcademicTerm term = new AcademicTerm();
        term.setId(UUID.randomUUID());
        term.setCode("FALL2026");
        AcademicBatch batch = new AcademicBatch();
        batch.setId(UUID.randomUUID());
        batch.setTerm(term);
        batch.setCode("BATCH-2026-A");
        batch.setName("Batch 2026 Section A");
        return batch;
    }

    private User student() {
        User student = new User();
        student.setId(UUID.randomUUID());
        student.setFullName("Demo Student");
        student.setEmail("student@onnorokom.com");
        student.setRole(UserRole.Student);
        student.setActive(true);
        return student;
    }

    @Test
    void create_unknownTerm_shouldThrowDoesNotExist() {
        UUID termId = UUID.randomUUID();
        when(termRepository.findById(termId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> batchService()
                .create(new CreateBatchRequest(termId, "BATCH-X", "Batch X")));
        assertEquals("Academic term with ID '" + termId + "' does not exist.", ex.getMessage());
    }

    @Test
    void assignStudent_unknownBatch_shouldThrowWasNotFound() {
        UUID batchId = UUID.randomUUID();
        when(batchRepository.findWithTermById(batchId)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> batchService()
                .assignStudent(batchId, new AssignStudentRequest(UUID.randomUUID())));
        assertEquals("Batch with ID '" + batchId + "' was not found.", ex.getMessage());
    }

    @Test
    void assignStudent_nonStudent_shouldThrowRoleMessage() {
        AcademicBatch batch = batchWithTerm();
        when(batchRepository.findWithTermById(batch.getId())).thenReturn(Optional.of(batch));
        User teacher = student();
        teacher.setRole(UserRole.Teacher);
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> batchService()
                .assignStudent(batch.getId(), new AssignStudentRequest(teacher.getId())));
        assertEquals("Assigned user must exist and have the Student role.", ex.getMessage());
    }

    @Test
    void assignStudent_duplicate_shouldThrowAlreadyAssigned() {
        AcademicBatch batch = batchWithTerm();
        User student = student();
        when(batchRepository.findWithTermById(batch.getId())).thenReturn(Optional.of(batch));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByBatchIdAndStudentId(batch.getId(), student.getId()))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> batchService()
                .assignStudent(batch.getId(), new AssignStudentRequest(student.getId())));
    }

    @Test
    void assignStudent_activeElsewhereInTerm_shouldThrow() {
        AcademicBatch batch = batchWithTerm();
        User student = student();
        when(batchRepository.findWithTermById(batch.getId())).thenReturn(Optional.of(batch));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByBatchIdAndStudentId(any(), any())).thenReturn(false);
        when(enrollmentRepository.existsActiveInTerm(student.getId(), batch.getTerm().getId()))
                .thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> batchService()
                .assignStudent(batch.getId(), new AssignStudentRequest(student.getId())));
        assertEquals("Student already has an active batch enrollment in this academic term.",
                ex.getMessage());
    }

    @Test
    void assignStudent_success_shouldForceActive() {
        AcademicBatch batch = batchWithTerm();
        User student = student();
        when(batchRepository.findWithTermById(batch.getId())).thenReturn(Optional.of(batch));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByBatchIdAndStudentId(any(), any())).thenReturn(false);
        when(enrollmentRepository.existsActiveInTerm(any(), any())).thenReturn(false);
        when(enrollmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var created = batchService()
                .assignStudent(batch.getId(), new AssignStudentRequest(student.getId()));

        assertEquals("Active", created.status());
        assertEquals("Demo Student", created.studentName());
    }

    @Test
    void setEnrollmentStatus_unknown_shouldThrowEnrollmentNotFound() {
        UUID id = UUID.randomUUID();
        when(enrollmentRepository.findWithBatchById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> batchService()
                .setEnrollmentStatus(id, new SetBatchEnrollmentStatusRequest(EnrollmentStatus.Inactive)));
        assertEquals("Enrollment Not Found", ex.getTitle());
    }

    @Test
    void getBatchStudents_unknownBatch_shouldReturnEmptyList() {
        UUID batchId = UUID.randomUUID();
        when(enrollmentRepository.findByBatchIdWithStudentOrderByStudentFullName(batchId))
                .thenReturn(List.of());

        assertTrue(batchService().getBatchStudents(batchId).isEmpty());
    }
}
