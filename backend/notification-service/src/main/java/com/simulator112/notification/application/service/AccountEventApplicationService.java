package com.simulator112.notification.application.service;

import com.simulator112.notification.application.port.in.ProcessAccountEventsUseCase;
import com.simulator112.notification.application.port.out.MailDelivery;
import com.simulator112.notification.application.port.out.UserDirectory;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountEventApplicationService implements ProcessAccountEventsUseCase {
    private final MailDelivery mailDelivery;
    private final UserDirectory users;

    @Override
    public void sendPasswordReset(UUID userId, String email, String resetLink) {
        String body = "Для сброса пароля перейдите по ссылке:\n" + resetLink
                + "\n\nСсылка действительна в течение 1 часа.";
        mailDelivery.send(null, userId, email, "Сброс пароля", body);
    }

    @Override
    public void sendEmailVerification(UUID userId, String email, String verificationLink) {
        String body = "Добро пожаловать в наш проект simulator112! Для подтверждения email перейдите по ссылке:\n"
                + verificationLink + "\n\nСсылка действительна в течение 3 часов.";
        mailDelivery.send(null, userId, email, "Подтверждение email", body);
    }

    @Override
    public void registerUser(UUID userId, String email) {
        users.upsert(userId, email);
    }
}
