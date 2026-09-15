package com.simulator112.incident.dto.view;

import com.simulator112.incident.model.enums.FieldType;

import java.util.UUID;

public record ClassifierFieldView(
        UUID id,
        String name,
        FieldType type,
        boolean required
) {
}
