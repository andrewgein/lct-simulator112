package com.simulator112.classifier.adapter.in.web.dto;

import java.util.List;
import java.util.UUID;

public record ClassifierEntryView(
        UUID id,
        String code,
        List<ClassifierFeatureView> features,
        String statisticalGroup,
        String additionalFeatures,
        String finalName,
        String ekp35Name,
        List<DispatchServiceView> primaryServices,
        List<String> routingFactCodes
) {
}
