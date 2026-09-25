package com.simulator112.incident.application.service;

public class IncidentGenerationException extends RuntimeException {
    public IncidentGenerationException(String message) {
        super(message);
    }

    public IncidentGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
