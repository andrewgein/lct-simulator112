package com.simulator112.incident.dto.request.embeddable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record VictimRequest(
        @NotBlank(message = "Имя обязательно")
        String firstName,

        @NotBlank(message = "Фамилия обязательна")
        String lastName,

        String middleName,

        @Positive(message = "Возраст должен быть положительным")
        Integer age,

        @NotBlank(message = "Телефон обязателен")
        String phone,

        String contactPhone,

        String address,

        String additionalInfo
) {
}
