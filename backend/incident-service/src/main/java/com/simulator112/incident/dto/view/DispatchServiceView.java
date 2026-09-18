package com.simulator112.incident.dto.view;

import java.util.UUID;

public record DispatchServiceView(
        UUID id,
        String code,
        String name
) {
}
