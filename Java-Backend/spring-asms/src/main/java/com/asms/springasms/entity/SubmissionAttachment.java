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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "submission_attachments")
@Getter
@Setter
@NoArgsConstructor
public class SubmissionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "`SubmissionId`", nullable = false)
    private Submission submission;

    @Column(name = "`OriginalFileName`", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "`ContentType`", nullable = false, length = 150)
    private String contentType;

    @Column(name = "`ByteSize`", nullable = false)
    private long byteSize;

    @Column(name = "`FileData`", nullable = false, columnDefinition = "bytea")
    private byte[] fileData;
}
