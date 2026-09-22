package com.simulator112.course.domain.exception;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(UUID incidentId) {
        super("Происшествие не найдено: " + incidentId);
    }
}
