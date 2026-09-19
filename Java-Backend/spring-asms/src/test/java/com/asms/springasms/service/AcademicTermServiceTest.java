package com.asms.springasms.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.asms.springasms.dto.term.CreateAcademicTermRequest;
import com.asms.springasms.dto.term.UpdateAcademicTermRequest;
import com.asms.springasms.entity.AcademicTerm;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AcademicBatchRepository;
import com.asms.springasms.repository.AcademicTermRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicTermServiceTest {

    @Mock
    private AcademicTermRepository termRepository;
    @Mock
    private AcademicBatchRepository batchRepository;

    private AcademicTermService termService() {
        return new AcademicTermService(termRepository, batchRepository);
    }

    @Test
    void create_startAfterEnd_shouldThrow() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> termService()
                .create(new CreateAcademicTermRequest("FALL2026",
                        LocalDate.of(2026, 12, 31), LocalDate.of(2026, 9, 1))));
        assertEquals("Term start date cannot be after end date.", ex.getMessage());
    }

    @Test
    void create_duplicateCode_shouldThrowWithRawCode() {
        when(termRepository.existsByCodeIgnoreCase("FALL2026")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> termService()
                .create(new CreateAcademicTermRequest(" fall2026 ",
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31))));
        assertEquals("Academic term with code ' fall2026 ' already exists.", ex.getMessage());
    }

    @Test
    void create_success_shouldUppercaseCode() {
        when(termRepository.existsByCodeIgnoreCase(any())).thenReturn(false);
        when(termRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var created = termService().create(new CreateAcademicTermRequest("fall2026",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31)));

        assertEquals("FALL2026", created.code());
    }

    @Test
    void update_unknown_shouldThrowTermNotFound() {
        UUID id = UUID.randomUUID();
        when(termRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> termService()
                .update(id, new UpdateAcademicTermRequest("FALL2026",
                        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31))));
        assertEquals("Academic Term Not Found", ex.getTitle());
    }

    @Test
    void delete_referencedByBatches_shouldThrow() {
        AcademicTerm term = new AcademicTerm();
        term.setId(UUID.randomUUID());
        when(termRepository.findById(term.getId())).thenReturn(Optional.of(term));
        when(batchRepository.existsByTermId(term.getId())).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> termService().delete(term.getId()));
        assertEquals("Cannot delete academic term because batches reference it.", ex.getMessage());
        verify(termRepository, never()).delete(any());
    }
}
