package com.asms.springasms.repository;

import com.asms.springasms.entity.CourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {

    @Query("""
            select ce from CourseEnrollment ce
            join fetch ce.batchEnrollment be
            join fetch be.student s
            join fetch be.batch b
            where ce.course.id = :courseId
            order by s.fullName
            """)
    List<CourseEnrollment> findCourseStudents(@Param("courseId") UUID courseId);

    @Query("""
            select ce from CourseEnrollment ce
            join fetch ce.course c
            where ce.batchEnrollment.student.id = :studentId
            order by c.code
            """)
    List<CourseEnrollment> findStudentCourses(@Param("studentId") UUID studentId);

    @Query("""
            select ce.course.id from CourseEnrollment ce
            where ce.batchEnrollment.student.id = :studentId
              and ce.status = com.asms.springasms.enums.EnrollmentStatus.Active
              and ce.batchEnrollment.status = com.asms.springasms.enums.EnrollmentStatus.Active
            """)
    List<UUID> findActiveCourseIdsByStudentId(@Param("studentId") UUID studentId);

    @Query("""
            select case when count(ce) > 0 then true else false end
            from CourseEnrollment ce
            where ce.course.id = :courseId
              and ce.batchEnrollment.student.id = :studentId
              and ce.status = com.asms.springasms.enums.EnrollmentStatus.Active
              and ce.batchEnrollment.status = com.asms.springasms.enums.EnrollmentStatus.Active
            """)
    boolean existsActiveEnrollment(@Param("courseId") UUID courseId, @Param("studentId") UUID studentId);

    boolean existsByCourseId(UUID courseId);

    List<CourseEnrollment> findByCourseIdAndBatchEnrollmentIdIn(UUID courseId,
                                                                Collection<UUID> batchEnrollmentIds);
}
