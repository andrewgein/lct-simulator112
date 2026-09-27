package com.simulator112.notification.adapter.out.persistence.repository;

import com.simulator112.notification.adapter.out.persistence.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipient(String recipient);

    List<Message> findByUserId(UUID userId);

    boolean existsByEventId(UUID eventId);
}
