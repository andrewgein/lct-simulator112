package com.simulator112.notification.application.port.out;

import java.util.UUID;

public interface MailDelivery {
    void send(UUID eventId, UUID userId, String recipient, String subject, String body);
}
