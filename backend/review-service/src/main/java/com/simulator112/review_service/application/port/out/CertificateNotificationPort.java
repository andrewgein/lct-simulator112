package com.simulator112.review_service.application.port.out;

import com.simulator112.review_service.domain.model.Certificate;

public interface CertificateNotificationPort {
    void publish(Certificate certificate);
}
