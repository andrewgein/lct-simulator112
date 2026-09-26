package com.simulator112.review_service.application.exception;

import java.util.UUID;

public class CertificateNotFoundException extends RuntimeException {
    public CertificateNotFoundException(UUID id) {
        super("Сертификат не найден: " + id);
    }
}
