package com.simulator112.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.simulator112.auth.dto.event.EmailVerificationRequestedEvent;
import com.simulator112.auth.dto.event.PasswordResetRequestedEvent;
import com.simulator112.auth.dto.event.RoleChangedEvent;
import com.simulator112.auth.dto.event.UserCreatedEvent;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private static final String TOPIC_EMAIL_VERIFICATION = "email.verification.requested";
    private static final String TOPIC_PASSWORD_RESET = "password.reset.requested";
    private static final String TOPIC_USER_CREATED = "user.created";
    private static final String TOPIC_AUDIT_DOMAIN = "audit.domain.events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Kafka -> {}: userId={}", TOPIC_PASSWORD_RESET, event.getUserId());
        kafkaTemplate.send(TOPIC_PASSWORD_RESET, String.valueOf(event.getUserId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Ошибка отправки в {}: userId={}", TOPIC_PASSWORD_RESET, event.getUserId(), ex);
                    }
                });
    }

    public void publishEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Kafka -> {}: userId={}", TOPIC_EMAIL_VERIFICATION, event.getUserId());
        kafkaTemplate.send(TOPIC_EMAIL_VERIFICATION, String.valueOf(event.getUserId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Ошибка отправки в {}: userId={}", TOPIC_EMAIL_VERIFICATION, event.getUserId(), ex);
                    }
                });
    }

    public void publishUserCreated(UUID userId, String email) {
        log.info("Kafka -> {}: userId={}", TOPIC_USER_CREATED, userId);
        kafkaTemplate.send(TOPIC_USER_CREATED, String.valueOf(userId), new UserCreatedEvent(userId, email ))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Ошибка отправки в {}: userId={}", TOPIC_USER_CREATED, userId, ex);
                    }
                });
    }

    public void publishRoleChanged(RoleChangedEvent event) {
        log.info("Kafka -> {}: resourceId={}, {}", TOPIC_AUDIT_DOMAIN, event.getResourceId(), event.getDetails());
        kafkaTemplate.send(TOPIC_AUDIT_DOMAIN, event.getResourceId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Ошибка отправки в {}: resourceId={}", TOPIC_AUDIT_DOMAIN, event.getResourceId(), ex);
                    }
                });
    }
}
