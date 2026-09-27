package com.simulator112.course.adapter.out.persistence.entity;

import com.simulator112.course.domain.model.CertificateType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
public class CertificateJpaEntity {
    @Id
    private UUID id;
    private UUID userId;
    private UUID courseId;
    private String courseTitle;
    private int percent;
    @Enumerated(EnumType.STRING)
    private CertificateType type;
    @Column(nullable = false, updatable = false)
    private Instant issuedAt;
}
