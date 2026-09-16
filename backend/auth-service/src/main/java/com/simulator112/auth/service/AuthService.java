package com.simulator112.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.simulator112.auth.configuration.JwtService;
import com.simulator112.auth.dto.event.EmailVerificationRequestedEvent;
import com.simulator112.auth.dto.event.PasswordResetRequestedEvent;
import com.simulator112.auth.dto.UserDetails;
import com.simulator112.auth.dto.request.ForgotPasswordRequest;
import com.simulator112.auth.dto.request.LoginRequest;
import com.simulator112.auth.dto.request.RegisterRequest;
import com.simulator112.auth.dto.request.ResetPasswordRequest;
import com.simulator112.auth.dto.request.VerifyEmailRequest;
import com.simulator112.auth.dto.response.AuthResponse;
import com.simulator112.auth.dto.response.ChangeRoleResponse;
import com.simulator112.auth.dto.response.UserResponse;
import com.simulator112.auth.exception.EmailAlreadyExistsException;
import com.simulator112.auth.exception.InvalidCredentialsException;
import com.simulator112.auth.exception.InvalidTokenException;
import com.simulator112.auth.model.entity.EmailVerificationToken;
import com.simulator112.auth.model.entity.PasswordResetToken;
import com.simulator112.auth.model.entity.RefreshToken;
import com.simulator112.auth.model.enums.Role;
import com.simulator112.auth.model.entity.User;
import com.simulator112.auth.repository.EmailVerificationTokenRepository;
import com.simulator112.auth.repository.PasswordResetTokenRepository;
import com.simulator112.auth.repository.RefreshTokenRepository;
import com.simulator112.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final KafkaEventPublisher kafkaEventPublisher;

    public void register(RegisterRequest request) {
        log.info("Попытка регистрации: email={}", request.getEmail());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Регистрация отклонена — email уже занят: {}", request.getEmail());
            throw new EmailAlreadyExistsException("Email уже используется");
        }
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .emailVerified(false)
                .build();
        userRepository.save(user);
        log.info("Пользователь зарегистрирован без подтверждения email: id={}, email={}", user.getId(), user.getEmail());

        String tokenValue = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken(tokenValue);
        verificationToken.setUserId(user.getId());
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(3));
        verificationToken.setUsed(false);
        emailVerificationTokenRepository.save(verificationToken);

        String link = frontendBaseUrl + "/verify-email?token=" + tokenValue;
        kafkaEventPublisher.publishEmailVerificationRequested(
                new EmailVerificationRequestedEvent(user.getId(), user.getEmail(), link));
        log.info("Ссылка подтверждения email отправлена: userId={}, link={}", user.getId(), link);
    }

    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("Подтверждение email отклонено — токен {} не найден", request.getToken());
                    return new InvalidTokenException("Токен не найден или уже использован");
                });

        if (verificationToken.isUsed()) {
            log.warn("Подтверждение email отклонено — токен уже использован: userId={}",
                    verificationToken.getUserId());
            throw new InvalidTokenException("Токен уже использован");
        }
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Подтверждение email отклонено — токен истёк: userId={}",
                    verificationToken.getUserId());
            throw new InvalidTokenException("Срок действия токена истёк");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));

        if (!user.isEmailVerified()) {
            Role role = userRepository.existsByEmailVerifiedTrue() ? Role.STUDENT: Role.ADMIN;
            user.setRole(role);
            user.setEmailVerified(true);
            userRepository.save(user);
            kafkaEventPublisher.publishUserCreated(user.getId(), user.getEmail());
            log.info("Email подтверждён: userId={}, role={}", user.getId(), role);
        }

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        return buildTokenPair(user.getId(), user.getEmail(), user.getRole().name());
    }
    

    public AuthResponse login(LoginRequest request) {
        log.info("Попытка входа: email={}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Вход не выполнен — email не найден: {}", request.getEmail());
                    return new InvalidCredentialsException("Неверный email или пароль");
                });
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Вход не выполнен — неверный пароль: email={}", request.getEmail());
            throw new InvalidCredentialsException("Неверный email или пароль");
        }
        if (!user.isEmailVerified()) {
            log.warn("Вход не выполнен — email не подтверждён: email={}", request.getEmail());
            throw new InvalidCredentialsException("Email не подтверждён");
        }
        log.info("Успешный вход: id={}, email={}", user.getId(), user.getEmail());
        return buildTokenPair(user.getId(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse refresh(String refreshToken) {
        log.debug("Запрос обновления access-токена");
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Обновление токена отклонено — refresh token отсутствует");
            throw new InvalidTokenException("Refresh token отсутствует");
        }
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Обновление токена отклонено — токен не найден");
                    return new InvalidTokenException("Токен не найден");
                });
        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Обновление токена отклонено — токен истёк или отозван: userId={}", token.getUserId());
            throw new InvalidTokenException("Токен истёк или отозван");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));
        if (!user.isEmailVerified()) {
            log.warn("Обновление токена отклонено — email не подтверждён: userId={}", user.getId());
            throw new InvalidCredentialsException("Email не подтверждён");
        }

        String newAccessToken = jwtService.generateAccessToken(
                new UserDetails(user.getEmail(), user.getId(), user.getRole().name()));
        log.info("Access-токен обновлён: userId={}", user.getId());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .role(user.getRole().name())
                .build();
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Выход выполнен: userId={}", token.getUserId());
        });
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Запрос сброса пароля: email={}", request.getEmail());
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.deleteAllByUserId(user.getId());

            String tokenValue = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(tokenValue);
            resetToken.setUserId(user.getId());
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            resetToken.setUsed(false);
            passwordResetTokenRepository.save(resetToken);

            String link = frontendBaseUrl + "/reset-password?token=" + tokenValue;
            kafkaEventPublisher.publishPasswordResetRequested(
                    new PasswordResetRequestedEvent(user.getId(), user.getEmail(), link));
            log.info("Ссылка сброса пароля отправлена: userId={}, link={}", user.getId(), link);
        });
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(u -> UserResponse.builder()
            .id(u.getId())
            .email(u.getEmail())
            .role(u.getRole().name())
            .createdAt(u.getCreatedAt())
            .build())
            .toList();
    }

    public String getUserRole(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));
        return user.getRole().name();
    }

    public ChangeRoleResponse changeRole(UUID userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));
        Role previousRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("Роль изменена: userId={}, {} -> {}", userId, previousRole, role);
        return ChangeRoleResponse.builder()
                .role(role.name())
                .build();
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("Сброс пароля отклонён — токен не найден");
                    return new InvalidTokenException("Токен не найден или уже использован");
                });

        if (resetToken.isUsed()) {
            log.warn("Сброс пароля отклонён — токен уже использован: userId={}", resetToken.getUserId());
            throw new InvalidTokenException("Токен уже использован");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Сброс пароля отклонён — токен истёк: userId={}", resetToken.getUserId());
            throw new InvalidTokenException("Срок действия токена истёк");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Пользователь не найден"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Пароль успешно сброшен: userId={}", user.getId());
    }

    private AuthResponse buildTokenPair(UUID userId, String email, String role) {
        refreshTokenRepository.deleteAllByUserId(userId);

        String accessToken = jwtService.generateAccessToken(new UserDetails(email, userId, role));
        String refreshTokenValue = jwtService.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .role(role)
                .refreshToken(refreshTokenValue)
                .build();
    }
}
