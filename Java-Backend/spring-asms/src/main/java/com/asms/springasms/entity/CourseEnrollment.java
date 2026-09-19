package com.asms.springasms.entity;

import com.asms.springasms.enums.EnrollmentStatus;
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
@Table(name = "course_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "IX_course_enrollments_BatchEnrollmentId_CourseId",
                columnNames = {"BatchEnrollmentId", "CourseId"}))
@Getter
@Setter
@NoArgsConstructor
public class CourseEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`BatchEnrollmentId`", nullable = false)
    private BatchEnrollment batchEnrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`CourseId`", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "`Status`", nullable = false, length = 16)
    private EnrollmentStatus status = EnrollmentStatus.Active;
}
