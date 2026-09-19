package com.asms.springasms.entity;

import com.asms.springasms.enums.TeacherCourseAllocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "teacher_course_allocations",
        uniqueConstraints = @UniqueConstraint(name = "IX_teacher_course_allocations_TeacherId_CourseId",
                columnNames = {"TeacherId", "CourseId"}))
@Getter
@Setter
@NoArgsConstructor
public class TeacherCourseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`TeacherId`", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`CourseId`", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "`Status`", nullable = false, length = 16)
    private TeacherCourseAllocationStatus status = TeacherCourseAllocationStatus.Active;
}
