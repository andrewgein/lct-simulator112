package com.simulator112.incident.dto.view.classifier;

import java.util.List;

public record ClassifierCategoryView(
        String code,
        String name,
        List<ClassifierEntryView> entries
) {
}
