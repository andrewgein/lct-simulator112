package com.simulator112.auth.application.service;

import com.simulator112.auth.application.port.in.DeleteUserUseCase;
import com.simulator112.auth.application.port.out.AccountStore;
import com.simulator112.auth.application.port.out.EmailVerificationStore;
import com.simulator112.auth.application.port.out.PasswordResetStore;
import com.simulator112.auth.application.port.out.RefreshSessionStore;
import com.simulator112.auth.domain.exception.InvalidCredentialsException;
import com.simulator112.auth.domain.exception.UserDeletionConflictException;
import com.simulator112.auth.domain.exception.UserNotFoundException;
import com.simulator112.auth.domain.model.Role;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdministrationService implements DeleteUserUseCase {
    private final AccountStore accounts;
    private final RefreshSessionStore refreshSessions;
    private final PasswordResetStore passwordResets;
    private final EmailVerificationStore emailVerifications;

    @Override
    @Transactional
    public void deleteUser(UUID userId, UUID actorUserId) {
        if (userId.equals(actorUserId)) throw new UserDeletionConflictException("Нельзя удалить собственную учётную запись");
        var administrators = accounts.lockAdministrators();
        if (administrators.stream().noneMatch(account -> account.id().equals(actorUserId) && account.emailVerified())) {
            throw new InvalidCredentialsException("Учётная запись администратора недействительна");
        }
        var user = accounts.findById(userId).orElseThrow(UserNotFoundException::new);
        if (user.role() == Role.ADMIN && administrators.size() <= 1) {
            throw new UserDeletionConflictException("Нельзя удалить последнего администратора");
        }
        refreshSessions.deleteAllByUserId(userId);
        passwordResets.deleteAllByUserId(userId);
        emailVerifications.deleteAllByUserId(userId);
        accounts.deleteById(userId);
    }
}
