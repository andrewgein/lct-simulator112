package com.simulator112.notification.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateIssuedEvent {
    private UUID eventId;
    private UUID certificateId;
    private UUID recipientUserId;
    private String courseTitle;
    private int percent;
    private String type;
    private Instant createdAt;
}
