package com.simulator112.notification.application.port.out;

import com.simulator112.notification.domain.model.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationStore {
    Optional<Notification> findByEventId(UUID eventId);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
    List<Notification> findForUser(UUID userId);
    long countUnread(UUID userId);
    int markAllRead(UUID userId, Instant readAt);
    Notification save(Notification notification);
}
