package com.simulator112.review_service.adapter.out.messaging;

import com.simulator112.review_service.adapter.out.messaging.dto.CertificateIssuedEvent;
import com.simulator112.review_service.application.port.out.CertificateNotificationPort;
import com.simulator112.review_service.domain.model.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCertificateNotificationAdapter implements CertificateNotificationPort {
    static final String TOPIC = "certificate.issued";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(Certificate certificate) {
        var event = new CertificateIssuedEvent(certificate.id(), certificate.id(), certificate.userId(),
                certificate.courseTitle(), certificate.percent(), certificate.type().name(), certificate.issuedAt());
        kafkaTemplate.send(TOPIC, certificate.userId().toString(), event).whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("Опубликовано событие выдачи сертификата: certificateId={}, userId={}",
                        certificate.id(), certificate.userId());
            } else {
                log.error("Не удалось опубликовать событие выдачи сертификата: certificateId={}",
                        certificate.id(), exception);
            }
        });
    }
}
