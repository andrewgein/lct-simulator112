package com.simulator112.incident.application.service;

public class ClassifierUnavailableException extends IncidentGenerationException {
    public ClassifierUnavailableException(Throwable cause) {
        super("Не удалось получить коды классификатора. Попробуйте позже", cause);
    }
}
