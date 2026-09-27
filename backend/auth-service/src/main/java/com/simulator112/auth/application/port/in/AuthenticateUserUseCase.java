package com.simulator112.auth.application.port.in;

import com.simulator112.auth.domain.model.Role;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuthenticateUserUseCase {
    void register(String email, String password);
    Tokens verifyEmail(String token);
    Tokens login(String email, String password);
    Tokens refresh(String refreshToken);
    void logout(String refreshToken);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    List<UserSummary> getAllUsers();
    String getUserRole(UUID userId);
    String changeRole(UUID userId, Role role, UUID actorUserId, String actorEmail, String actorRole);

    record Tokens(String accessToken, String refreshToken, String role) {}
    record UserSummary(UUID id, String email, String role, LocalDateTime createdAt) {}
}
