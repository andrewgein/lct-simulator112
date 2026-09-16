package com.simulator112.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.simulator112.auth.configuration.JwtService;
import com.simulator112.auth.dto.request.LoginRequest;
import com.simulator112.auth.dto.request.VerifyEmailRequest;
import com.simulator112.auth.dto.response.AuthResponse;
import com.simulator112.auth.dto.UserDetails;
import com.simulator112.auth.exception.InvalidCredentialsException;
import com.simulator112.auth.exception.InvalidTokenException;
import com.simulator112.auth.model.entity.EmailVerificationToken;
import com.simulator112.auth.model.entity.RefreshToken;
import com.simulator112.auth.model.enums.Role;
import com.simulator112.auth.model.entity.User;
import com.simulator112.auth.repository.EmailVerificationTokenRepository;
import com.simulator112.auth.repository.PasswordResetTokenRepository;
import com.simulator112.auth.repository.RefreshTokenRepository;
import com.simulator112.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private AuthService authService;

    private final UUID verifiedUserUUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86_400_000L);
    }

    @Test
    void loginReturnsTokenPairAndStoresRefreshToken() {
        User user = verifiedUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getRole()).isEqualTo("STUDENT");

        verify(refreshTokenRepository).deleteAllByUserId(user.getId());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("refresh-token");
        assertThat(savedToken.getUserId()).isEqualTo(verifiedUserUUID);
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void loginRejectsUnverifiedUserWithoutCreatingToken() {
        User user = verifiedUser();
        user.setEmailVerified(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email не подтверждён");

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyEmailReturnsTokenPairAndStoresRefreshToken() {
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setToken("verify-token");
        verificationToken.setUserId(verifiedUserUUID);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        verificationToken.setUsed(false);

        User user = verifiedUser();
        user.setEmailVerified(false);

        when(emailVerificationTokenRepository.findByToken("verify-token")).thenReturn(Optional.of(verificationToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailVerifiedTrue()).thenReturn(true);
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken()).thenReturn("refresh-token");

        AuthResponse response = authService.verifyEmail(new VerifyEmailRequest("verify-token"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getRole()).isEqualTo("STUDENT");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(verificationToken.isUsed()).isTrue();

        verify(userRepository).save(user);
        verify(emailVerificationTokenRepository).save(verificationToken);
        verify(refreshTokenRepository).deleteAllByUserId(user.getId());
        verify(kafkaEventPublisher).publishUserCreated(user.getId(), "user@example.com");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isEqualTo("refresh-token");
    }

    @Test
    void refreshReturnsNewAccessTokenForValidRefreshToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUserId(verifiedUserUUID);
        refreshToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        refreshToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(verifiedUserUUID)).thenReturn(Optional.of(verifiedUser()));
        when(jwtService.generateAccessToken(any(UserDetails.class))).thenReturn("new-access-token");

        AuthResponse response = authService.refresh("refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getRefreshToken()).isNull();
        assertThat(response.getRole()).isEqualTo("STUDENT");
    }

    @Test
    void refreshRejectsRevokedToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUserId(verifiedUserUUID);
        refreshToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        refreshToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Токен истёк или отозван");

        verify(jwtService, never()).generateAccessToken(any());
    }

    private User verifiedUser() {
        return User.builder()
                .id(verifiedUserUUID)
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.STUDENT)
                .emailVerified(true)
                .build();
    }
}
