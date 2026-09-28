package com.simulator112.notification.adapter.out.mail;

import com.simulator112.notification.application.port.out.MailDelivery;
import com.simulator112.notification.adapter.out.persistence.entity.Message;
import com.simulator112.notification.adapter.out.persistence.repository.MessageRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpMailDeliveryAdapter implements MailDelivery {
    @Value("${spring.mail.username}")
    private String from;

    private final JavaMailSender mailSender;
    private final MessageRepository messageRepository;

    public void send(UUID eventId, UUID userId, String to, String subject, String body) {
        if (eventId != null && messageRepository.existsByEventId(eventId)) {
            log.info("Письмо для события {} уже отправлено", eventId);
            return;
        }
        log.info("Отправка письма на {} с темой '{}'", to, subject);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject(subject);
        mailMessage.setText(body);
        mailMessage.setFrom(from);

        try {
            mailSender.send(mailMessage);
            log.info("Письмо успешно отправлено на {}", to);
        } catch (MailException e) {
            log.error("Ошибка отправки письма на {}: {}", to, e.getMessage(), e);
            throw e;
        }

        Message message = new Message();
        message.setSender(from);
        message.setRecipient(to);
        message.setText(body);
        message.setSubject(subject);
        message.setUserId(userId);
        message.setEventId(eventId);

        try {
            Message savedMessage = messageRepository.save(message);
            log.info("Сообщение сохранено в БД с id={} для {}", savedMessage.getId(), to);
        } catch (Exception e) {
            log.error("Ошибка сохранения сообщения в БД (письмо уже отправлено): {}", e.getMessage(), e);
            throw e;
        }
    }
}
