package com.simulator112.incident.dto.view;

import com.simulator112.incident.model.enums.ServiceType;

import java.util.UUID;

public record TypeView(
        UUID id,
        ServiceType serviceType,
        String typeId,
        String typeName
) {
}
