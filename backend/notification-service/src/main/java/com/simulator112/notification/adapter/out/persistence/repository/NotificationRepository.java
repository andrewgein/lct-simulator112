package com.simulator112.notification.adapter.out.persistence.repository;

import com.simulator112.notification.adapter.out.persistence.NotificationJpaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    Optional<NotificationJpaEntity> findByEventId(UUID eventId);

    Optional<NotificationJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    List<NotificationJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :readAt WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
