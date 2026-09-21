package com.simulator112.incident.domain.common.exception;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(UUID id) {
        super("Происшествие с id " + id + " не найдено");
    }

}
