package com.simulator112.notification.adapter.in.kafka.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationRequestedEvent {
    private UUID userId;
    private String email;
    private String verificationLink;
}
