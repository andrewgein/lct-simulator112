package com.simulator112.review_service.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateResponse(UUID id, UUID courseId, String courseTitle, int percent, String type,
                                  Instant issuedAt) {
}
