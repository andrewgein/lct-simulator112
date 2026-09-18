package com.simulator112.incident.dto.view;

import java.util.List;

public record ClassifierCategoryView(
        String code,
        String name,
        List<ClassifierEntryView> entries
) {
}
