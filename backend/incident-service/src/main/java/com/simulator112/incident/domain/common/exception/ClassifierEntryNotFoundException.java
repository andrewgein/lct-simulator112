package com.simulator112.incident.domain.common.exception;

public class ClassifierEntryNotFoundException extends RuntimeException {

    public ClassifierEntryNotFoundException(String code) {
        super("Тип происшествия с кодом " + code + " не найден");
    }
}
