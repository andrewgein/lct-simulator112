package com.simulator112.notification.application.model;

import java.time.Instant;
import java.util.UUID;

public record CertificateNotice(UUID eventId, UUID recipientUserId, UUID certificateId,
                                String type, String courseTitle, int percent, Instant createdAt) {}
