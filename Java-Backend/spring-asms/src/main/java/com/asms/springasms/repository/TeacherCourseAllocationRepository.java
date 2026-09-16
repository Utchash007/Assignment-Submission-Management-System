package com.asms.springasms.repository;

import com.asms.springasms.entity.TeacherCourseAllocation;
import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TeacherCourseAllocationRepository extends JpaRepository<TeacherCourseAllocation, UUID> {

    @Query("""
            select a from TeacherCourseAllocation a
            join fetch a.teacher t
            where a.course.id = :courseId
            order by t.fullName
            """)
    List<TeacherCourseAllocation> findCourseTeachers(@Param("courseId") UUID courseId);

    @Query("""
            select a from TeacherCourseAllocation a
            join fetch a.course c
            where a.teacher.id = :teacherId
            order by c.code
            """)
    List<TeacherCourseAllocation> findTeacherCourses(@Param("teacherId") UUID teacherId);

    @Query("""
            select a.course.id from TeacherCourseAllocation a
            where a.teacher.id = :teacherId
              and a.status = com.asms.springasms.enums.TeacherCourseAllocationStatus.Active
            """)
    List<UUID> findActiveCourseIdsByTeacherId(@Param("teacherId") UUID teacherId);

    boolean existsByTeacherIdAndCourseId(UUID teacherId, UUID courseId);

    boolean existsByCourseId(UUID courseId);

    boolean existsByTeacherIdAndCourseIdAndStatus(UUID teacherId,
                                                  UUID courseId,
                                                  TeacherCourseAllocationStatus status);
}
