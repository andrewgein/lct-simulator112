package com.simulator112.notification.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Notification {
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String text;
    private String targetUrl;
    private Instant createdAt;
    private Instant readAt;
    private EmailDeliveryStatus emailStatus;
    private Instant emailSentAt;

    public boolean isRead() {
        return readAt != null;
    }
}
