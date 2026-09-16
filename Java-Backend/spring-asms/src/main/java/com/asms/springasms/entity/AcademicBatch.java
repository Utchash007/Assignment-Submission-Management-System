package com.asms.springasms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "academic_batches",
        uniqueConstraints = @UniqueConstraint(name = "IX_academic_batches_TermId_Code",
                columnNames = {"TermId", "Code"}))
@Getter
@Setter
@NoArgsConstructor
public class AcademicBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`TermId`", nullable = false)
    private AcademicTerm term;

    @Column(name = "`Code`", nullable = false, length = 50)
    private String code;

    @Column(name = "`Name`", nullable = false, length = 200)
    private String name;
}
