package com.simulator112.incident.dto.view.classifier;

import java.util.List;
import java.util.UUID;

public record ClassifierEntryView(
        UUID id,
        String code,
        String categoryCode,
        String categoryName,
        String feature1Code,
        String feature1Name,
        String feature2Code,
        String feature2Name,
        String feature3Code,
        String feature3Name,
        String statisticalGroup,
        String additionalFeatures,
        String finalName,
        String ekp35Name,
        List<DispatchServiceView> primaryServices
) {
}
