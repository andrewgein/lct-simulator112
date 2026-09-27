package com.simulator112.notification.domain.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("Уведомление не найдено");
    }
}
