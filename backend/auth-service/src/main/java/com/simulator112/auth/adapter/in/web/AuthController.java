package com.simulator112.auth.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import com.simulator112.auth.adapter.in.web.dto.ApiResponse;
import com.simulator112.auth.adapter.in.web.dto.request.ChangeRoleRequest;
import com.simulator112.auth.adapter.in.web.dto.request.ForgotPasswordRequest;
import com.simulator112.auth.adapter.in.web.dto.request.LoginRequest;
import com.simulator112.auth.adapter.in.web.dto.request.RegisterRequest;
import com.simulator112.auth.adapter.in.web.dto.request.ResetPasswordRequest;
import com.simulator112.auth.adapter.in.web.dto.request.VerifyEmailRequest;
import com.simulator112.auth.adapter.in.web.dto.response.AuthResponse;
import com.simulator112.auth.adapter.in.web.dto.response.ChangeRoleResponse;
import com.simulator112.auth.adapter.in.web.dto.response.UserResponse;
import com.simulator112.auth.application.port.in.AuthenticateUserUseCase;

@RestController
public class AuthController {

  private final AuthenticateUserUseCase authService;

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  public AuthController(AuthenticateUserUseCase authService) {
    this.authService = authService;
  }

  @PostMapping("/api/v1/auth/change-role/{userId}")
  public ResponseEntity<ApiResponse<ChangeRoleResponse>> changeRole(
      @Valid @RequestBody ChangeRoleRequest request,
      @PathVariable UUID userId,
      @RequestHeader(value = "X-User-Id", required = false) UUID actorUserId,
      @RequestHeader(value = "X-User-Email", required = false) String actorEmail,
      @RequestHeader(value = "X-User-Role", required = false) String actorRole) {
    ChangeRoleResponse changeRoleResponse =
        ChangeRoleResponse.builder().role(authService.changeRole(userId, request.getRole(), actorUserId, actorEmail, actorRole)).build();
    return ResponseEntity.ok(
        ApiResponse.<ChangeRoleResponse>builder()
            .success(true)
            .message("Роль успешно изменена")
            .status(HttpStatus.OK.value())
            .data(changeRoleResponse)
            .timestamp(LocalDateTime.now())
            .build());
  }

  @PostMapping("/api/v1/auth/register")
  public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
    authService.register(request.getEmail(), request.getPassword());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.<Void>builder()
                .success(true)
                .message("Проверьте почту для подтверждения аккаунта")
                .status(HttpStatus.CREATED.value())
                .timestamp(LocalDateTime.now())
                .build());
  }

  @PostMapping("/api/v1/auth/verify-email")
  public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(
      @Valid @RequestBody VerifyEmailRequest request, HttpServletResponse response) {
    AuthResponse authResponse = toResponse(authService.verifyEmail(request.getToken()));
    setRefreshTokenCookie(response, authResponse.getRefreshToken());
    return ResponseEntity.ok(
        ApiResponse.<AuthResponse>builder()
            .success(true)
            .message("Email подтверждён")
            .status(HttpStatus.OK.value())
            .data(authResponse)
            .timestamp(LocalDateTime.now())
            .build());
  }

  @PostMapping("/api/v1/auth/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
    AuthResponse authResponse = toResponse(authService.login(request.getEmail(), request.getPassword()));
    setRefreshTokenCookie(response, authResponse.getRefreshToken());
    return ResponseEntity.ok(
        ApiResponse.<AuthResponse>builder()
            .success(true)
            .message("Вход выполнен успешно")
            .status(HttpStatus.OK.value())
            .data(authResponse)
            .timestamp(LocalDateTime.now())
            .build());
  }

  @PostMapping("/api/v1/auth/refresh")
  public ResponseEntity<ApiResponse<AuthResponse>> refresh(
      @CookieValue(value = "refreshToken", required = false) String refreshToken) {
    AuthResponse authResponse = toResponse(authService.refresh(refreshToken));
    return ResponseEntity.ok(
        ApiResponse.<AuthResponse>builder()
            .success(true)
            .message("Токен обновлён")
            .status(HttpStatus.OK.value())
            .data(authResponse)
            .timestamp(LocalDateTime.now())
            .build());
  }

  @PostMapping("/api/v1/auth/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @CookieValue(value = "refreshToken", required = false) String refreshToken,
      HttpServletResponse response) {
    if (refreshToken != null) {
      authService.logout(refreshToken);
    }
    clearRefreshTokenCookie(response);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Выход выполнен успешно")
            .status(HttpStatus.OK.value())
            .timestamp(LocalDateTime.now())
            .build());
  }

  @PostMapping("/api/v1/auth/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request.getEmail());
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Если email найден — ссылка для сброса отправлена")
            .status(HttpStatus.OK.value())
            .timestamp(LocalDateTime.now())
            .build());
  }

  @PostMapping("/api/v1/auth/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request.getToken(), request.getNewPassword());
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .success(true)
            .message("Пароль успешно изменён")
            .status(HttpStatus.OK.value())
            .timestamp(LocalDateTime.now())
            .build());
  }

  @GetMapping("/api/v1/admin/users")
  public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
    List<UserResponse> users = authService.getAllUsers().stream()
        .map(user -> UserResponse.builder().id(user.id()).email(user.email()).role(user.role())
            .createdAt(user.createdAt()).build()).toList();
    return ResponseEntity.ok(
        ApiResponse.<List<UserResponse>>builder()
            .success(true)
            .message("Список пользователей")
            .status(HttpStatus.OK.value())
            .data(users)
            .timestamp(LocalDateTime.now())
            .build());
  }

  @GetMapping("/api/v1/users/{userId}/role")
  public ResponseEntity<ApiResponse<String>> getUserRole(@PathVariable UUID userId) {
    String role = authService.getUserRole(userId);
    return ResponseEntity.ok(
        ApiResponse.<String>builder()
            .success(true)
            .message("Роль пользователя")
            .status(HttpStatus.OK.value())
            .data(role)
            .timestamp(LocalDateTime.now())
            .build());
  }

  private void setRefreshTokenCookie(HttpServletResponse response, String token) {
    Cookie cookie = new Cookie("refreshToken", token);
    cookie.setHttpOnly(true);
    cookie.setSecure(false);
    cookie.setPath("/api/v1/auth");
    cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
    response.addCookie(cookie);
  }

  private AuthResponse toResponse(AuthenticateUserUseCase.Tokens tokens) {
    return AuthResponse.builder().accessToken(tokens.accessToken()).refreshToken(tokens.refreshToken())
        .tokenType("Bearer").role(tokens.role()).build();
  }

  private void clearRefreshTokenCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie("refreshToken", "");
    cookie.setHttpOnly(true);
    cookie.setPath("/api/v1/auth");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }
}
