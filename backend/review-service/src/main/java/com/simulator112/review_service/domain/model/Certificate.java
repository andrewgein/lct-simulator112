package com.simulator112.review_service.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Certificate(UUID id, UUID userId, UUID courseId, String courseTitle, int percent,
                          CertificateType type, Instant issuedAt) {
}
