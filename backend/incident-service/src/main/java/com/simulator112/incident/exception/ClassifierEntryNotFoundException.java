package com.simulator112.incident.exception;

public class ClassifierEntryNotFoundException extends RuntimeException {

    public ClassifierEntryNotFoundException(String code) {
        super("Тип происшествия с кодом " + code + " не найден");
    }
}
