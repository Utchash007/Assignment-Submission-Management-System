package com.asms.springasms.repository;

import com.asms.springasms.entity.Assignment;
import com.asms.springasms.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("select a from Assignment a where a.id = :id and a.deletedAt is null")
    Optional<Assignment> findActiveById(@Param("id") UUID id);

    @Query("""
            select a from Assignment a
            join fetch a.course c
            join fetch a.createdBy
            where a.id = :id and a.deletedAt is null
            """)
    Optional<Assignment> findActiveWithDetailsById(@Param("id") UUID id);

    @Query("""
            select a from Assignment a
            join fetch a.course c
            where a.id = :id and a.deletedAt is null
            """)
    Optional<Assignment> findActiveWithCourseById(@Param("id") UUID id);

    @Query("""
            select a from Assignment a
            join fetch a.course c
            join fetch a.createdBy
            where a.deletedAt is null
            order by a.deadlineAt desc
            """)
    List<Assignment> findAllActiveWithDetailsOrderByDeadlineAtDesc();

    @Query("""
            select a from Assignment a
            join fetch a.course c
            join fetch a.createdBy
            where a.deletedAt is null and a.course.id = :courseId
            order by a.deadlineAt desc
            """)
    List<Assignment> findActiveByCourseIdWithDetails(@Param("courseId") UUID courseId);

    @Query("""
            select a from Assignment a
            join fetch a.course c
            join fetch a.createdBy
            where a.deletedAt is null and a.course.id in :courseIds
            order by a.deadlineAt desc
            """)
    List<Assignment> findActiveByCourseIdInWithDetails(@Param("courseIds") Collection<UUID> courseIds);

    @Query("""
            select a from Assignment a
            join fetch a.course c
            join fetch a.createdBy
            where a.deletedAt is null
              and a.status = :status
              and a.course.id in :courseIds
            order by a.deadlineAt desc
            """)
    List<Assignment> findActiveByStatusAndCourseIdInWithDetails(@Param("status") AssignmentStatus status,
                                                                @Param("courseIds") Collection<UUID> courseIds);

    @Query("select case when count(a) > 0 then true else false end from Assignment a where a.course.id = :courseId")
    boolean existsByCourseIdIncludingDeleted(@Param("courseId") UUID courseId);
}
