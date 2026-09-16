package com.asms.springasms.service;

import com.asms.springasms.dto.term.AcademicTermResponse;
import com.asms.springasms.dto.term.CreateAcademicTermRequest;
import com.asms.springasms.dto.term.UpdateAcademicTermRequest;
import com.asms.springasms.entity.AcademicTerm;
import com.asms.springasms.exception.NotFoundException;
import com.asms.springasms.repository.AcademicBatchRepository;
import com.asms.springasms.repository.AcademicTermRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AcademicTermService {

    private final AcademicTermRepository termRepository;
    private final AcademicBatchRepository batchRepository;

    @Transactional(readOnly = true)
    public List<AcademicTermResponse> getAll() {
        return termRepository.findAllByOrderByStartsOnDesc().stream()
                .map(AcademicTermResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AcademicTermResponse getById(UUID id) {
        return termRepository.findById(id)
                .map(AcademicTermResponse::from)
                .orElseThrow(() -> new NotFoundException("Academic Term Not Found",
                        "Academic term with ID '" + id + "' was not found."));
    }

    @Transactional
    public AcademicTermResponse create(CreateAcademicTermRequest request) {
        if (request.startsOn().isAfter(request.endsOn())) {
            throw new IllegalStateException("Term start date cannot be after end date.");
        }
        String normalizedCode = request.code().trim().toUpperCase();
        if (termRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new IllegalStateException(
                    "Academic term with code '" + request.code() + "' already exists.");
        }
        AcademicTerm term = new AcademicTerm();
        term.setCode(normalizedCode);
        term.setStartsOn(request.startsOn());
        term.setEndsOn(request.endsOn());
        return AcademicTermResponse.from(termRepository.save(term));
    }

    @Transactional
    public AcademicTermResponse update(UUID id, UpdateAcademicTermRequest request) {
        if (request.startsOn().isAfter(request.endsOn())) {
            throw new IllegalStateException("Term start date cannot be after end date.");
        }
        AcademicTerm term = termRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Academic Term Not Found",
                        "Academic term with ID '" + id + "' was not found."));
        String normalizedCode = request.code().trim().toUpperCase();
        if (!term.getCode().equalsIgnoreCase(normalizedCode)
                && termRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            throw new IllegalStateException(
                    "Academic term with code '" + request.code() + "' already exists.");
        }
        term.setCode(normalizedCode);
        term.setStartsOn(request.startsOn());
        term.setEndsOn(request.endsOn());
        return AcademicTermResponse.from(term);
    }

    @Transactional
    public void delete(UUID id) {
        AcademicTerm term = termRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Academic Term Not Found",
                        "Academic term with ID '" + id + "' was not found."));
        if (batchRepository.existsByTermId(id)) {
            throw new IllegalStateException("Cannot delete academic term because batches reference it.");
        }
        termRepository.delete(term);
    }
}
