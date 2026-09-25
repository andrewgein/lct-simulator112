package com.simulator112.notification.dto;

import java.util.List;

public record NotificationsResponse(List<NotificationResponse> notifications) {
    public NotificationsResponse {
        notifications = List.copyOf(notifications);
    }
}
