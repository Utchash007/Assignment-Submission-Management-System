package com.asms.springasms.repository;

import com.asms.springasms.entity.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, UUID> {

    @Query("""
            select a from SubmissionAttachment a
            join fetch a.submission s
            join fetch s.assignment asg
            where a.id = :id and asg.deletedAt is null
            """)
    Optional<SubmissionAttachment> findActiveByIdWithSubmissionAndAssignment(@Param("id") UUID id);

    @Query("""
            select a from SubmissionAttachment a
            join fetch a.submission s
            join fetch s.assignment asg
            where s.id = :submissionId and asg.deletedAt is null
            """)
    List<SubmissionAttachment> findActiveBySubmissionId(@Param("submissionId") UUID submissionId);
}
