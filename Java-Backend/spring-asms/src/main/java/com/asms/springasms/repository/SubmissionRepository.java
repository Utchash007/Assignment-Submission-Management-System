package com.asms.springasms.repository;

import com.asms.springasms.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    @Query("""
            select distinct s from Submission s
            join fetch s.assignment a
            join fetch s.student st
            left join fetch s.evaluatedBy
            left join fetch s.attachments
            where a.deletedAt is null and st.id = :studentId
            order by s.submittedAt desc
            """)
    List<Submission> findActiveByStudentIdWithDetails(@Param("studentId") UUID studentId);

    @Query("""
            select distinct s from Submission s
            join fetch s.assignment a
            join fetch s.student st
            left join fetch s.evaluatedBy
            left join fetch s.attachments
            where a.deletedAt is null and a.id = :assignmentId
            order by s.submittedAt desc
            """)
    List<Submission> findActiveByAssignmentIdWithDetails(@Param("assignmentId") UUID assignmentId);

    @Query("""
            select distinct s from Submission s
            join fetch s.assignment a
            join fetch s.student st
            left join fetch s.evaluatedBy
            left join fetch s.attachments
            where a.deletedAt is null and s.id = :id
            """)
    Optional<Submission> findActiveByIdWithDetails(@Param("id") UUID id);

    @Query("""
            select distinct s from Submission s
            left join fetch s.attachments
            where s.assignment.id = :assignmentId
              and s.student.id = :studentId
              and s.assignment.deletedAt is null
            """)
    Optional<Submission> findActiveByAssignmentIdAndStudentIdWithAttachments(
            @Param("assignmentId") UUID assignmentId,
            @Param("studentId") UUID studentId);

    @Query("""
            select s from Submission s
            join fetch s.assignment a
            where s.id = :id and a.deletedAt is null
            """)
    Optional<Submission> findActiveWithAssignmentById(@Param("id") UUID id);

    @Query("""
            select case when count(s) > 0 then true else false end
            from Submission s
            where s.assignment.id = :assignmentId
              and s.assignment.deletedAt is null
            """)
    boolean existsActiveByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
