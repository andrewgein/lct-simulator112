package com.simulator112.auth.application.service;

import com.simulator112.auth.application.port.out.AccountStore;
import com.simulator112.auth.application.port.out.AuthEventPublisher;
import com.simulator112.auth.application.port.out.EmailVerificationStore;
import com.simulator112.auth.application.port.out.PasswordHasher;
import com.simulator112.auth.application.port.out.PasswordResetStore;
import com.simulator112.auth.application.port.out.RefreshSessionStore;
import com.simulator112.auth.application.port.out.TokenIssuer;
import com.simulator112.auth.domain.model.Account;
import com.simulator112.auth.domain.model.OneTimeToken;
import com.simulator112.auth.domain.model.RefreshSession;
import com.simulator112.auth.domain.exception.InvalidCredentialsException;
import com.simulator112.auth.domain.exception.InvalidTokenException;
import com.simulator112.auth.domain.model.Role;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {
    @Mock private AccountStore accounts;
    @Mock private RefreshSessionStore refreshSessions;
    @Mock private PasswordResetStore passwordResets;
    @Mock private EmailVerificationStore emailVerifications;
    @Mock private TokenIssuer tokens;
    @Mock private PasswordHasher passwords;
    @Mock private AuthEventPublisher events;
    @InjectMocks private AuthApplicationService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenExpiration", 86_400_000L);
    }

    @Test
    void loginReturnsTokenPairAndStoresRefreshSession() {
        when(accounts.findByEmail("user@example.com")).thenReturn(Optional.of(verifiedUser()));
        when(passwords.matches("password", "encoded-password")).thenReturn(true);
        when(tokens.issueAccess(userId, "user@example.com", "STUDENT")).thenReturn("access-token");
        when(tokens.issueRefresh()).thenReturn("refresh-token");

        var result = service.login("user@example.com", "password");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.role()).isEqualTo("STUDENT");
        verify(refreshSessions).deleteAllByUserId(userId);
        ArgumentCaptor<RefreshSession> saved = ArgumentCaptor.forClass(RefreshSession.class);
        verify(refreshSessions).save(saved.capture());
        assertThat(saved.getValue().userId()).isEqualTo(userId);
        assertThat(saved.getValue().expiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void loginRejectsUnverifiedUser() {
        Account user = new Account(userId, "user@example.com", "encoded-password", Role.STUDENT, false, null);
        when(accounts.findByEmail(user.email())).thenReturn(Optional.of(user));
        when(passwords.matches("password", user.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> service.login(user.email(), "password"))
                .isInstanceOf(InvalidCredentialsException.class).hasMessage("Email не подтверждён");
        verify(tokens, never()).issueAccess(any(), any(), any());
    }

    @Test
    void verifyEmailActivatesUserAndPublishesEvent() {
        OneTimeToken verification = new OneTimeToken("verify-token", userId, LocalDateTime.now().plusHours(1), false);
        Account unverified = new Account(userId, "user@example.com", "encoded-password", Role.STUDENT, false, null);
        when(emailVerifications.findByToken("verify-token")).thenReturn(Optional.of(verification));
        when(accounts.findById(userId)).thenReturn(Optional.of(unverified));
        when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokens.issueAccess(userId, "user@example.com", "ADMIN")).thenReturn("access-token");
        when(tokens.issueRefresh()).thenReturn("refresh-token");

        var result = service.verifyEmail("verify-token");

        assertThat(result.role()).isEqualTo("ADMIN");
        verify(events).userCreated(userId, "user@example.com");
        verify(emailVerifications).save(verification.markUsed());
        verify(refreshSessions).deleteAllByUserId(userId);
    }

    @Test
    void refreshReturnsAccessOnly() {
        RefreshSession session = new RefreshSession("refresh-token", userId, LocalDateTime.now().plusHours(1), false);
        when(refreshSessions.findByToken(session.token())).thenReturn(Optional.of(session));
        when(accounts.findById(userId)).thenReturn(Optional.of(verifiedUser()));
        when(tokens.issueAccess(userId, "user@example.com", "STUDENT")).thenReturn("new-access-token");

        var result = service.refresh(session.token());

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isNull();
    }

    @Test
    void refreshRejectsRevokedToken() {
        RefreshSession session = new RefreshSession("refresh-token", userId, LocalDateTime.now().plusHours(1), true);
        when(refreshSessions.findByToken(session.token())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.refresh(session.token()))
                .isInstanceOf(InvalidTokenException.class).hasMessage("Токен истёк или отозван");
        verify(tokens, never()).issueAccess(any(), any(), any());
    }

    private Account verifiedUser() {
        return new Account(userId, "user@example.com", "encoded-password", Role.STUDENT, true, null);
    }
}
