package com.simulator112.incident.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " с id " + id + " не найден");
    }
}
