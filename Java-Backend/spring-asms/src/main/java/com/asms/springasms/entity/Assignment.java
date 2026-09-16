package com.asms.springasms.entity;

import com.asms.springasms.enums.AssignmentStatus;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`CourseId`", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`CreatedByUserId`", nullable = false)
    private User createdBy;

    @Column(name = "`Title`", nullable = false, length = 250)
    private String title;

    @Column(name = "`Description`", columnDefinition = "text")
    private String description;

    @Column(name = "`DeadlineAt`", nullable = false)
    private Instant deadlineAt;

    @Column(name = "`MaximumMarks`", nullable = false, precision = 8, scale = 2)
    private BigDecimal maximumMarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "`Status`", nullable = false, length = 16)
    private AssignmentStatus status = AssignmentStatus.Draft;

    @Column(name = "`AllowResubmission`", nullable = false)
    private boolean allowResubmission;

    @Column(name = "`SubmissionsClosedAt`")
    private Instant submissionsClosedAt;

    @Column(name = "`DeletedAt`")
    private Instant deletedAt;
}
