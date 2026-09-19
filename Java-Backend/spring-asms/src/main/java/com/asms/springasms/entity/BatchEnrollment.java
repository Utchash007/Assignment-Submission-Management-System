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
@Table(name = "batch_enrollments",
        uniqueConstraints = @UniqueConstraint(name = "IX_batch_enrollments_StudentId_BatchId",
                columnNames = {"StudentId", "BatchId"}))
@Getter
@Setter
@NoArgsConstructor
public class BatchEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`BatchId`", nullable = false)
    private AcademicBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`StudentId`", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "`Status`", nullable = false, length = 16)
    private EnrollmentStatus status = EnrollmentStatus.Active;
}
