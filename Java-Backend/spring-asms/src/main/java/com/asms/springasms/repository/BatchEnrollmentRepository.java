package com.asms.springasms.repository;

import com.asms.springasms.entity.BatchEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BatchEnrollmentRepository extends JpaRepository<BatchEnrollment, UUID> {

    @Query("""
            select e from BatchEnrollment e
            join fetch e.student s
            where e.batch.id = :batchId
            order by s.fullName
            """)
    List<BatchEnrollment> findByBatchIdWithStudentOrderByStudentFullName(@Param("batchId") UUID batchId);

    @Query("select e from BatchEnrollment e join fetch e.batch where e.id = :id")
    Optional<BatchEnrollment> findWithBatchById(@Param("id") UUID id);

    @Query("select e from BatchEnrollment e join fetch e.student join fetch e.batch where e.id in :ids")
    List<BatchEnrollment> findAllByIdInWithStudentAndBatch(@Param("ids") Collection<UUID> ids);

    boolean existsByBatchId(UUID batchId);

    boolean existsByBatchIdAndStudentId(UUID batchId, UUID studentId);

    @Query("""
            select case when count(e) > 0 then true else false end
            from BatchEnrollment e
            where e.student.id = :studentId
              and e.batch.term.id = :termId
              and e.status = com.asms.springasms.enums.EnrollmentStatus.Active
            """)
    boolean existsActiveInTerm(@Param("studentId") UUID studentId, @Param("termId") UUID termId);

    @Query("""
            select case when count(e) > 0 then true else false end
            from BatchEnrollment e
            where e.student.id = :studentId
              and e.batch.term.id = :termId
              and e.status = com.asms.springasms.enums.EnrollmentStatus.Active
              and e.id <> :excludeId
            """)
    boolean existsActiveInTermExcluding(@Param("studentId") UUID studentId,
                                        @Param("termId") UUID termId,
                                        @Param("excludeId") UUID excludeId);
}
