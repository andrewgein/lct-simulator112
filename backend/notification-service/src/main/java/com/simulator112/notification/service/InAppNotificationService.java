package com.simulator112.notification.service;

import com.simulator112.notification.dto.CertificateIssuedEvent;
import com.simulator112.notification.dto.ReviewCommentCreatedEvent;
import com.simulator112.notification.model.EmailDeliveryStatus;
import com.simulator112.notification.model.Notification;
import com.simulator112.notification.model.NotificationType;
import com.simulator112.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class InAppNotificationService {
    private static final String REVIEW_COMMENT_TITLE = "Новый комментарий к результату";

    private final NotificationRepository repository;
    private final UserService userService;
    private final MailSenderService mailSenderService;

    @Value("${notification.app-base-url:http://localhost:4321}")
    private String appBaseUrl;

    public void handleReviewComment(ReviewCommentCreatedEvent event) {
        Notification notification = repository.findByEventId(event.getEventId())
                .orElseGet(() -> repository.save(newReviewCommentNotification(event)));
        if (notification.getEmailStatus() == EmailDeliveryStatus.SENT) return;

        try {
            String email = userService.getEmailById(event.getRecipientUserId());
            String targetUrl = normalizeBaseUrl(appBaseUrl) + notification.getTargetUrl();
            String body = "Преподаватель оставил комментарий к результату прохождения.\n\n"
                    + "Комментарий:\n" + event.getCommentText() + "\n\n"
                    + "Открыть результат:\n" + targetUrl;
            mailSenderService.send(event.getEventId(), event.getRecipientUserId(), email,
                    REVIEW_COMMENT_TITLE, body);
            notification.setEmailStatus(EmailDeliveryStatus.SENT);
            notification.setEmailSentAt(Instant.now());
            repository.save(notification);
        } catch (RuntimeException exception) {
            notification.setEmailStatus(EmailDeliveryStatus.FAILED);
            repository.save(notification);
            throw exception;
        }
    }

    public void handleCertificateIssued(CertificateIssuedEvent event) {
        repository.findByEventId(event.getEventId())
                .orElseGet(() -> repository.save(newCertificateNotification(event)));
    }

    @Transactional(readOnly = true)
    public List<Notification> getForUser(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public Notification markRead(UUID notificationId, UUID userId) {
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Уведомление не найдено"));
        if (!notification.isRead()) {
            notification.setReadAt(Instant.now());
            notification = repository.save(notification);
        }
        return notification;
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId, Instant.now());
    }

    private Notification newReviewCommentNotification(ReviewCommentCreatedEvent event) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setEventId(event.getEventId());
        notification.setUserId(event.getRecipientUserId());
        notification.setType(NotificationType.REVIEW_COMMENT);
        notification.setTitle(REVIEW_COMMENT_TITLE);
        notification.setText(event.getCommentText().trim());
        notification.setTargetUrl("/review/" + event.getReviewContextId() + "#comments");
        notification.setCreatedAt(event.getCreatedAt() == null ? Instant.now() : event.getCreatedAt());
        notification.setEmailStatus(EmailDeliveryStatus.PENDING);
        return notification;
    }

    private Notification newCertificateNotification(CertificateIssuedEvent event) {
        boolean honors = "HONORS".equals(event.getType());
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setEventId(event.getEventId());
        notification.setUserId(event.getRecipientUserId());
        notification.setType(NotificationType.CERTIFICATE_ISSUED);
        notification.setTitle(honors ? "Сертификат с отличием получен" : "Сертификат получен");
        notification.setText("Курс «" + event.getCourseTitle() + "» пройден на " + event.getPercent() + "%. "
                + (honors ? "Вам выдан сертификат с отличием." : "Вам выдан сертификат о прохождении."));
        notification.setTargetUrl("/profile/certificates/" + event.getCertificateId());
        notification.setCreatedAt(event.getCreatedAt() == null ? Instant.now() : event.getCreatedAt());
        notification.setEmailStatus(EmailDeliveryStatus.SENT);
        return notification;
    }

    private String normalizeBaseUrl(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
