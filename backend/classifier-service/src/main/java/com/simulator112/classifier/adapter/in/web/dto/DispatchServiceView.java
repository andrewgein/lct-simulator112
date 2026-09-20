package com.simulator112.classifier.adapter.in.web.dto;

import java.util.UUID;

public record DispatchServiceView(
        UUID id,
        String code,
        String name
) {
}
