package com.simulator112.incident.dto.request;

import com.simulator112.incident.dto.request.embeddable.AddressRequest;
import com.simulator112.incident.dto.request.embeddable.DispatcherCriteriaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateIncidentRequest(
    @NotBlank(message = "Название происшествия обязательно")
    String title,

    @NotNull(message = "Адрес обязателен")
    @Valid
    AddressRequest address,

    @NotNull(message = "Критерии оценки диспетчера обязательны")
    @Valid
    DispatcherCriteriaRequest criteria
) {
}
