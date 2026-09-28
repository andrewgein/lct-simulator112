package com.simulator112.notification.adapter.in.web;

import com.simulator112.notification.adapter.in.web.dto.NotificationResponse;
import com.simulator112.notification.adapter.in.web.dto.NotificationsResponse;
import com.simulator112.notification.adapter.in.web.dto.UnreadCountResponse;
import com.simulator112.notification.domain.model.Notification;
import com.simulator112.notification.application.port.in.ManageNotificationsUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final ManageNotificationsUseCase notificationService;

    @GetMapping
    public NotificationsResponse getNotifications(@RequestHeader("X-User-Id") UUID userId) {
        return new NotificationsResponse(notificationService.getForUser(userId).stream()
                .map(NotificationController::toResponse).toList());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@RequestHeader("X-User-Id") UUID userId) {
        return new UnreadCountResponse(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@RequestHeader("X-User-Id") UUID userId,
                                         @PathVariable UUID notificationId) {
        return toResponse(notificationService.markRead(notificationId, userId));
    }

    @PatchMapping("/read-all")
    public UnreadCountResponse markAllRead(@RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllRead(userId);
        return new UnreadCountResponse(0);
    }

    private static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getText(), notification.getTargetUrl(), notification.getCreatedAt(),
                notification.isRead());
    }
}
