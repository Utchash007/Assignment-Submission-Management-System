package com.asms.springasms.repository;

import com.asms.springasms.entity.AcademicBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicBatchRepository extends JpaRepository<AcademicBatch, UUID> {

    @Query("select b from AcademicBatch b join fetch b.term order by b.code")
    List<AcademicBatch> findAllWithTermOrderByCodeAsc();

    @Query("select b from AcademicBatch b join fetch b.term where b.id = :id")
    Optional<AcademicBatch> findWithTermById(@Param("id") UUID id);

    boolean existsByTermId(UUID termId);
}
