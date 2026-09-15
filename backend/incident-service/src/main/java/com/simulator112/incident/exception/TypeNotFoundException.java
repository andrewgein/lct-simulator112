package com.simulator112.incident.exception;

import java.util.UUID;

public class TypeNotFoundException extends RuntimeException {

    public TypeNotFoundException(UUID id) {
        super("Тип происшествия с id " + id + " не найден");
    }
}
