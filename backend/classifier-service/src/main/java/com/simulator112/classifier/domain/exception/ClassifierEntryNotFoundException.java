package com.simulator112.classifier.domain.exception;

public class ClassifierEntryNotFoundException extends RuntimeException {

    public ClassifierEntryNotFoundException(String code) {
        super("Запись классификатора с кодом " + code + " не найдена");
    }
}
