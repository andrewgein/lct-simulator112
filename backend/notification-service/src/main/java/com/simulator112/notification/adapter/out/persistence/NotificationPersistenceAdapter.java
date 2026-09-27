package com.simulator112.notification.adapter.out.persistence;

import com.simulator112.notification.application.port.out.NotificationStore;
import com.simulator112.notification.domain.model.Notification;
import com.simulator112.notification.adapter.out.persistence.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationPersistenceAdapter implements NotificationStore {
    private final NotificationRepository repository;

    public Optional<Notification> findByEventId(UUID eventId) {
        return repository.findByEventId(eventId).map(NotificationPersistenceMapper::toDomain);
    }

    public Optional<Notification> findByIdAndUserId(UUID id, UUID userId) {
        return repository.findByIdAndUserId(id, userId).map(NotificationPersistenceMapper::toDomain);
    }

    public List<Notification> findForUser(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationPersistenceMapper::toDomain).toList();
    }

    public long countUnread(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    public int markAllRead(UUID userId, Instant readAt) {
        return repository.markAllRead(userId, readAt);
    }

    public Notification save(Notification notification) {
        return NotificationPersistenceMapper.toDomain(repository.save(NotificationPersistenceMapper.toEntity(notification)));
    }
}
