package com.simulator112.notification.adapter.in.kafka;

import com.simulator112.notification.adapter.in.kafka.dto.CertificateIssuedEvent;
import com.simulator112.notification.adapter.in.kafka.dto.EmailVerificationRequestedEvent;
import com.simulator112.notification.adapter.in.kafka.dto.PasswordResetEvent;
import com.simulator112.notification.adapter.in.kafka.dto.ReviewCommentCreatedEvent;
import com.simulator112.notification.adapter.in.kafka.dto.UserCreatedEvent;
import com.simulator112.notification.application.model.CertificateNotice;
import com.simulator112.notification.application.model.ReviewCommentNotice;
import com.simulator112.notification.application.port.in.ManageNotificationsUseCase;
import com.simulator112.notification.application.port.in.ProcessAccountEventsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final ProcessAccountEventsUseCase accountEvents;
    private final ManageNotificationsUseCase notificationService;

    @KafkaListener(topics = "password.reset.requested", groupId = "notification-service")
    public void consumePasswordReset(PasswordResetEvent event) {

        log.info("Получен запрос сброса пароля для: {}", event.getEmail());

        accountEvents.sendPasswordReset(event.getUserId(), event.getEmail(), event.getResetLink());
    }

    @KafkaListener(topics = "email.verification.requested", groupId = "notification-service")
    public void consumeEmailVerification(EmailVerificationRequestedEvent event) {

        log.info("Получен запрос подтверждения email для: {}", event.getEmail());

        accountEvents.sendEmailVerification(event.getUserId(), event.getEmail(), event.getVerificationLink());
    }

    @KafkaListener(topics = "user.created", groupId = "notification-service")
    public void consumeUserCreated(UserCreatedEvent event) {

        log.info("Получено событие создания подтверждённого пользователя: userId={}, email={}",
                event.getUserId(), event.getEmail());
                
        accountEvents.registerUser(event.getUserId(), event.getEmail());
    }

    @KafkaListener(topics = "review.comment.created", groupId = "notification-service")
    public void consumeReviewCommentCreated(ReviewCommentCreatedEvent event) {
        log.info("Получено событие комментария к результату: eventId={}, recipientUserId={}",
                event.getEventId(), event.getRecipientUserId());
        notificationService.handleReviewComment(new ReviewCommentNotice(event.getEventId(),
                event.getRecipientUserId(), event.getReviewContextId(), event.getCommentText(), event.getCreatedAt()));
    }

    @KafkaListener(topics = "certificate.issued", groupId = "notification-service")
    public void consumeCertificateIssued(CertificateIssuedEvent event) {
        log.info("Получено событие выдачи сертификата: certificateId={}, recipientUserId={}",
                event.getCertificateId(), event.getRecipientUserId());
        notificationService.handleCertificateIssued(new CertificateNotice(event.getEventId(),
                event.getRecipientUserId(), event.getCertificateId(), event.getType(),
                event.getCourseTitle(), event.getPercent(), event.getCreatedAt()));
    }
}
