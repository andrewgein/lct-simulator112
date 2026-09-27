package com.simulator112.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token обязателен")
    private String token;

    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 6, message = "Новый пароль не менее 6 символов")
    private String newPassword;
}
