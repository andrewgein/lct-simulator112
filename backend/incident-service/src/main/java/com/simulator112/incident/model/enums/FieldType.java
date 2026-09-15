package com.simulator112.incident.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum FieldType {
    TEXT,
    TEXTAREA,
    NUMBER,
    BOOLEAN;

    @JsonValue
    public String getValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
