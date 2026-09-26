package com.simulator112.review_service.adapter.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record CertificateIssuedEvent(UUID eventId, UUID certificateId, UUID recipientUserId, String courseTitle,
                                     int percent, String type, Instant createdAt) {
}
