package com.simulator112.course.adapter.out.messaging;

import com.simulator112.course.adapter.out.messaging.dto.CertificateIssuedEvent;
import com.simulator112.course.domain.model.Certificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaCertificateNotificationAdapter {
    private final KafkaTemplate<String, Object> kafka;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(Certificate certificate) {
        var event = new CertificateIssuedEvent(certificate.id(), certificate.id(), certificate.userId(),
                certificate.courseTitle(), certificate.percent(), certificate.type().name(), certificate.issuedAt());
        try {
            kafka.send("certificate.issued", certificate.userId().toString(), event).whenComplete((result, error) -> {
                if (error != null) log.error("Не удалось отправить уведомление о сертификате {}", certificate.id(), error);
            });
        } catch (RuntimeException error) {
            log.error("Не удалось отправить уведомление о сертификате {}", certificate.id(), error);
        }
    }
}
