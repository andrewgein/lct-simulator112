package com.simulator112.auth.application.service;

import com.simulator112.auth.application.port.in.ValidateSessionUseCase;
import com.simulator112.auth.application.port.out.AccountStore;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionValidationService implements ValidateSessionUseCase {
    private final AccountStore accounts;

    @Override
    @Transactional(readOnly = true)
    public boolean isSessionValid(UUID userId, String role) {
        return accounts.findById(userId).filter(user -> user.emailVerified() && user.role().name().equals(role)).isPresent();
    }
}
