package com.simulator112.auth.application.service;

import com.simulator112.auth.application.port.in.AuthenticateUserUseCase;
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
import com.simulator112.auth.domain.exception.EmailAlreadyExistsException;
import com.simulator112.auth.domain.exception.InvalidCredentialsException;
import com.simulator112.auth.domain.exception.InvalidTokenException;
import com.simulator112.auth.domain.model.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthApplicationService implements AuthenticateUserUseCase {
    private final AccountStore accounts;
    private final RefreshSessionStore refreshSessions;
    private final PasswordResetStore passwordResets;
    private final EmailVerificationStore emailVerifications;
    private final TokenIssuer tokens;
    private final PasswordHasher passwords;
    private final AuthEventPublisher events;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void register(String email, String password) {
        if (accounts.findByEmail(email).isPresent()) throw new EmailAlreadyExistsException("Email уже используется");
        Account user = accounts.save(new Account(null, email, passwords.hash(password), Role.STUDENT, false, null));
        String value = UUID.randomUUID().toString();
        emailVerifications.save(new OneTimeToken(value, user.id(), LocalDateTime.now().plusHours(3), false));
        events.emailVerificationRequested(user.id(), user.email(), frontendBaseUrl + "/verify-email?token=" + value);
    }

    @Override
    @Transactional
    public Tokens verifyEmail(String value) {
        OneTimeToken token = emailVerifications.findByToken(value)
                .orElseThrow(() -> new InvalidTokenException("Токен не найден или уже использован"));
        requireUsable(token);
        Account user = requireAccount(token.userId());
        if (!user.emailVerified()) {
            Role role = accounts.anyVerified() ? Role.STUDENT : Role.ADMIN;
            user = accounts.save(user.verify(role));
            events.userCreated(user.id(), user.email());
        }
        emailVerifications.save(token.markUsed());
        return tokenPair(user);
    }

    @Override
    @Transactional
    public Tokens login(String email, String password) {
        Account user = accounts.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Неверный email или пароль"));
        if (!passwords.matches(password, user.passwordHash()))
            throw new InvalidCredentialsException("Неверный email или пароль");
        if (!user.emailVerified()) throw new InvalidCredentialsException("Email не подтверждён");
        return tokenPair(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Tokens refresh(String value) {
        if (value == null || value.isBlank()) throw new InvalidTokenException("Refresh token отсутствует");
        RefreshSession session = refreshSessions.findByToken(value)
                .orElseThrow(() -> new InvalidTokenException("Токен не найден"));
        if (session.revoked() || session.expiresAt().isBefore(LocalDateTime.now()))
            throw new InvalidTokenException("Токен истёк или отозван");
        Account user = requireAccount(session.userId());
        if (!user.emailVerified()) throw new InvalidCredentialsException("Email не подтверждён");
        return new Tokens(tokens.issueAccess(user.id(), user.email(), user.role().name()), null, user.role().name());
    }

    @Override
    @Transactional
    public void logout(String value) {
        refreshSessions.findByToken(value).ifPresent(session -> refreshSessions.save(session.revoke()));
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        accounts.findByEmail(email).ifPresent(user -> {
            passwordResets.deleteAllByUserId(user.id());
            String value = UUID.randomUUID().toString();
            passwordResets.save(new OneTimeToken(value, user.id(), LocalDateTime.now().plusHours(1), false));
            events.passwordResetRequested(user.id(), user.email(), frontendBaseUrl + "/reset-password?token=" + value);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String value, String newPassword) {
        OneTimeToken token = passwordResets.findByToken(value)
                .orElseThrow(() -> new InvalidTokenException("Токен не найден или уже использован"));
        requireUsable(token);
        accounts.save(requireAccount(token.userId()).withPassword(passwords.hash(newPassword)));
        passwordResets.save(token.markUsed());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> getAllUsers() {
        return accounts.findAll().stream()
                .map(user -> new UserSummary(user.id(), user.email(), user.role().name(), user.createdAt())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String getUserRole(UUID userId) {
        return requireAccount(userId).role().name();
    }

    @Override
    @Transactional
    public String changeRole(UUID userId, Role role, UUID actorUserId, String actorEmail, String actorRole) {
        Account user = requireAccount(userId);
        accounts.save(user.withRole(role));
        refreshSessions.deleteAllByUserId(userId);
        events.roleChanged(actorUserId, actorEmail, actorRole, userId, user.role().name(), role.name());
        return role.name();
    }

    private Account requireAccount(UUID userId) {
        return accounts.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));
    }

    private void requireUsable(OneTimeToken token) {
        if (token.used()) throw new InvalidTokenException("Токен уже использован");
        if (token.expiresAt().isBefore(LocalDateTime.now()))
            throw new InvalidTokenException("Срок действия токена истёк");
    }

    private Tokens tokenPair(Account user) {
        refreshSessions.deleteAllByUserId(user.id());
        String access = tokens.issueAccess(user.id(), user.email(), user.role().name());
        String refresh = tokens.issueRefresh();
        refreshSessions.save(new RefreshSession(refresh, user.id(),
                LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000), false));
        return new Tokens(access, refresh, user.role().name());
    }
}
