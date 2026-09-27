package com.simulator112.incident.application.service;

public class IncidentGenerationLimitException extends IncidentGenerationException {
    public IncidentGenerationLimitException() {
        super("Ответ модели превысил лимит. Попробуйте запросить меньше этапов или звонков");
    }
}
