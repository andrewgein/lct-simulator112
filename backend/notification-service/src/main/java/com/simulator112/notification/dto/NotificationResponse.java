package com.simulator112.notification.dto;

import com.simulator112.notification.model.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, NotificationType type, String title, String text,
                                   String targetUrl, Instant createdAt, boolean read) {
}
