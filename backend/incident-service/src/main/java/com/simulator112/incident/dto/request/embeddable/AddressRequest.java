package com.simulator112.incident.dto.request.embeddable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record AddressRequest(

        @NotBlank(message = "Город обязателен")
        String city,

        @NotBlank(message = "Улица обязательна")
        String street,

        @NotBlank(message = "Номер дома обязателен")
        String house,

        String building,

        String apartment,

        @PositiveOrZero(message = "Этаж не может быть отрицательным")
        Integer floor
) {
}
