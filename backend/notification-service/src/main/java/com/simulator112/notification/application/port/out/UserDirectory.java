package com.simulator112.notification.application.port.out;

import java.util.UUID;

public interface UserDirectory {
    void upsert(UUID userId, String email);
    String requireEmail(UUID userId);
}
