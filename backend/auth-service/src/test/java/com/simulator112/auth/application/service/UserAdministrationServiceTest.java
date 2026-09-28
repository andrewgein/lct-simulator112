package com.simulator112.auth.application.service;

import com.simulator112.auth.application.port.out.AccountStore;
import com.simulator112.auth.application.port.out.EmailVerificationStore;
import com.simulator112.auth.application.port.out.PasswordResetStore;
import com.simulator112.auth.application.port.out.RefreshSessionStore;
import com.simulator112.auth.domain.exception.InvalidCredentialsException;
import com.simulator112.auth.domain.exception.UserDeletionConflictException;
import com.simulator112.auth.domain.exception.UserNotFoundException;
import com.simulator112.auth.domain.model.Account;
import com.simulator112.auth.domain.model.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdministrationServiceTest {
    @Mock private AccountStore accounts;
    @Mock private RefreshSessionStore refreshSessions;
    @Mock private PasswordResetStore passwordResets;
    @Mock private EmailVerificationStore emailVerifications;
    @InjectMocks private UserAdministrationService service;
    private final UUID adminId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void deletesAccountAndAllTokens() {
        when(accounts.lockAdministrators()).thenReturn(List.of(account(adminId, Role.ADMIN)));
        when(accounts.findById(userId)).thenReturn(Optional.of(account(userId, Role.STUDENT)));

        service.deleteUser(userId, adminId);

        verify(refreshSessions).deleteAllByUserId(userId);
        verify(passwordResets).deleteAllByUserId(userId);
        verify(emailVerifications).deleteAllByUserId(userId);
        verify(accounts).deleteById(userId);
    }

    @Test
    void cannotDeleteSelfIncludingLastAdmin() {
        assertThatThrownBy(() -> service.deleteUser(adminId, adminId))
                .isInstanceOf(UserDeletionConflictException.class);
        verify(accounts, never()).deleteById(any());
    }

    @Test
    void canDeleteAnotherAdminWhenOneRemains() {
        when(accounts.lockAdministrators()).thenReturn(List.of(account(adminId, Role.ADMIN), account(userId, Role.ADMIN)));
        when(accounts.findById(userId)).thenReturn(Optional.of(account(userId, Role.ADMIN)));
        service.deleteUser(userId, adminId);
        verify(accounts).deleteById(userId);
    }

    @Test
    void missingUserIsNotFound() {
        when(accounts.lockAdministrators()).thenReturn(List.of(account(adminId, Role.ADMIN)));
        when(accounts.findById(userId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteUser(userId, adminId)).isInstanceOf(UserNotFoundException.class);
        verify(refreshSessions, never()).deleteAllByUserId(any());
    }

    @Test
    void deletedOrNonAdminActorCannotDeleteUsers() {
        when(accounts.lockAdministrators()).thenReturn(List.of());
        assertThatThrownBy(() -> service.deleteUser(userId, adminId)).isInstanceOf(InvalidCredentialsException.class);
        verify(accounts, never()).deleteById(any());
    }

    private Account account(UUID id, Role role) {
        return new Account(id, "user@example.com", "hash", role, true, null);
    }
}
