package com.simulator112.course.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateResponse(UUID id, UUID courseId, String courseTitle, int percent, String type,
                                  Instant issuedAt) {
}
