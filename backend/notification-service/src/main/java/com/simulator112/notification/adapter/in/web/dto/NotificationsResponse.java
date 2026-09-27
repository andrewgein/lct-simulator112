package com.simulator112.notification.adapter.in.web.dto;

import java.util.List;

public record NotificationsResponse(List<NotificationResponse> notifications) {
    public NotificationsResponse {
        notifications = List.copyOf(notifications);
    }
}
