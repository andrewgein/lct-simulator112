package com.simulator112.notification.application.port.in;

import com.simulator112.notification.application.model.CertificateNotice;
import com.simulator112.notification.application.model.ReviewCommentNotice;
import com.simulator112.notification.domain.model.Notification;
import java.util.List;
import java.util.UUID;

public interface ManageNotificationsUseCase {
    void handleReviewComment(ReviewCommentNotice event);
    void handleCertificateIssued(CertificateNotice event);
    List<Notification> getForUser(UUID userId);
    long getUnreadCount(UUID userId);
    Notification markRead(UUID notificationId, UUID userId);
    int markAllRead(UUID userId);
}
