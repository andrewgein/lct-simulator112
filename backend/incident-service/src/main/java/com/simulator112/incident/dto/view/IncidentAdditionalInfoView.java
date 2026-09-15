package com.simulator112.incident.dto.view;

import com.simulator112.incident.model.enums.FieldType;

import java.util.UUID;

public record IncidentAdditionalInfoView(
    UUID id,
    UUID additionalInfoId,
    String fieldCode,
    String fieldName,
    FieldType fieldType,
    boolean required,
    String fieldValue
) {
}
