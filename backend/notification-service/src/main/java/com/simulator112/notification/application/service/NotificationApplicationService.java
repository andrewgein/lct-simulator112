package com.simulator112.notification.application.service;

import com.simulator112.notification.application.port.in.ManageNotificationsUseCase;
import com.simulator112.notification.application.port.out.MailDelivery;
import com.simulator112.notification.application.port.out.NotificationStore;
import com.simulator112.notification.application.port.out.UserDirectory;
import com.simulator112.notification.application.model.CertificateNotice;
import com.simulator112.notification.application.model.ReviewCommentNotice;
import com.simulator112.notification.domain.model.EmailDeliveryStatus;
import com.simulator112.notification.domain.model.Notification;
import com.simulator112.notification.domain.model.NotificationType;
import com.simulator112.notification.domain.exception.NotificationNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationApplicationService implements ManageNotificationsUseCase {
    private static final String REVIEW_COMMENT_TITLE = "Новый комментарий к результату";

    private final NotificationStore repository;
    private final UserDirectory userService;
    private final MailDelivery mailSenderService;

    @Value("${notification.app-base-url:http://localhost:4321}")
    private String appBaseUrl;

    public void handleReviewComment(ReviewCommentNotice event) {
        Notification notification = repository.findByEventId(event.eventId())
                .orElseGet(() -> repository.save(newReviewCommentNotification(event)));
        if (notification.getEmailStatus() == EmailDeliveryStatus.SENT) return;

        try {
            String email = userService.requireEmail(event.recipientUserId());
            String targetUrl = normalizeBaseUrl(appBaseUrl) + notification.getTargetUrl();
            String body = "Преподаватель оставил комментарий к результату прохождения.\n\n"
                    + "Комментарий:\n" + event.commentText() + "\n\n"
                    + "Открыть результат:\n" + targetUrl;
            mailSenderService.send(event.eventId(), event.recipientUserId(), email,
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

    public void handleCertificateIssued(CertificateNotice event) {
        repository.findByEventId(event.eventId())
                .orElseGet(() -> repository.save(newCertificateNotification(event)));
    }

    @Transactional(readOnly = true)
    public List<Notification> getForUser(UUID userId) {
        return repository.findForUser(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return repository.countUnread(userId);
    }

    @Transactional
    public Notification markRead(UUID notificationId, UUID userId) {
        Notification notification = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(NotificationNotFoundException::new);
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

    private Notification newReviewCommentNotification(ReviewCommentNotice event) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setEventId(event.eventId());
        notification.setUserId(event.recipientUserId());
        notification.setType(NotificationType.REVIEW_COMMENT);
        notification.setTitle(REVIEW_COMMENT_TITLE);
        notification.setText(event.commentText().trim());
        notification.setTargetUrl("/review/" + event.reviewContextId() + "#comments");
        notification.setCreatedAt(event.createdAt() == null ? Instant.now() : event.createdAt());
        notification.setEmailStatus(EmailDeliveryStatus.PENDING);
        return notification;
    }

    private Notification newCertificateNotification(CertificateNotice event) {
        boolean honors = "HONORS".equals(event.type());
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setEventId(event.eventId());
        notification.setUserId(event.recipientUserId());
        notification.setType(NotificationType.CERTIFICATE_ISSUED);
        notification.setTitle(honors ? "Сертификат с отличием получен" : "Сертификат получен");
        notification.setText("Курс «" + event.courseTitle() + "» пройден на " + event.percent() + "%. "
                + (honors ? "Вам выдан сертификат с отличием." : "Вам выдан сертификат о прохождении."));
        notification.setTargetUrl("/profile/certificates/" + event.certificateId());
        notification.setCreatedAt(event.createdAt() == null ? Instant.now() : event.createdAt());
        notification.setEmailStatus(EmailDeliveryStatus.SENT);
        return notification;
    }

    private String normalizeBaseUrl(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
