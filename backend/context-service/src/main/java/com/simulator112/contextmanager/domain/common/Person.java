package com.simulator112.contextmanager.domain.common;

import com.simulator112.shared.dto.EmotionalState;

public record Person(String firstName, String lastName, String middleName, Integer age, String phone,
                     String contactPhone, String address, String additionalInfo, EmotionalState emotionalState) {
}
