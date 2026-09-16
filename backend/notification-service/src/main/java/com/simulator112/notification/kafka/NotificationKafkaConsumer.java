package com.simulator112.notification.kafka;

import com.simulator112.notification.dto.EmailVerificationRequestedEvent;
import com.simulator112.notification.dto.PasswordResetEvent;
import com.simulator112.notification.dto.UserCreatedEvent;
import com.simulator112.notification.service.MailSenderService;
import com.simulator112.notification.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final UserService userService;
    private final MailSenderService mailSenderService;

    @KafkaListener(topics = "password.reset.requested", groupId = "notification-service")
    public void consumePasswordReset(PasswordResetEvent event) {

        log.info("Получен запрос сброса пароля для: {}", event.getEmail());

        String subject = "Сброс пароля";
        String body = "Для сброса пароля перейдите по ссылке:\n" + event.getResetLink()
                + "\n\nСсылка действительна в течение 1 часа.";
        mailSenderService.send(null, event.getUserId(), event.getEmail(), subject, body);
    }

    @KafkaListener(topics = "email.verification.requested", groupId = "notification-service")
    public void consumeEmailVerification(EmailVerificationRequestedEvent event) {

        log.info("Получен запрос подтверждения email для: {}", event.getEmail());

        String subject = "Подтверждение email";
        String body = "Добро пожаловать в наш проект simulator112! Для подтверждения email перейдите по ссылке:\n"
                + event.getVerificationLink() + "\n\nСсылка действительна в течение 3 часов.";
        mailSenderService.send(null, event.getUserId(), event.getEmail(), subject, body);
    }

    @KafkaListener(topics = "user.created", groupId = "notification-service")
    public void consumeUserCreated(UserCreatedEvent event) {

        log.info("Получено событие создания подтверждённого пользователя: userId={}, email={}",
                event.getUserId(), event.getEmail());
                
        userService.userCreate(event.getUserId(), event.getEmail());
    }
}
