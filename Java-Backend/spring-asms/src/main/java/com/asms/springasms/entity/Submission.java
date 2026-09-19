package com.asms.springasms.entity;

import com.asms.springasms.enums.SubmissionStatus;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "submissions",
        uniqueConstraints = @UniqueConstraint(name = "IX_submissions_AssignmentId_StudentId",
                columnNames = {"AssignmentId", "StudentId"}))
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`AssignmentId`", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`StudentId`", nullable = false)
    private User student;

    @Column(name = "`AnswerText`", columnDefinition = "text")
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "`Status`", nullable = false, length = 16)
    private SubmissionStatus status = SubmissionStatus.Submitted;

    @Column(name = "`SubmittedAt`", nullable = false)
    private Instant submittedAt;

    @Column(name = "`Marks`", precision = 8, scale = 2)
    private BigDecimal marks;

    @Column(name = "`Feedback`", columnDefinition = "text")
    private String feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`EvaluatedByUserId`")
    private User evaluatedBy;

    @OneToMany(mappedBy = "submission")
    private List<SubmissionAttachment> attachments = new ArrayList<>();
}
