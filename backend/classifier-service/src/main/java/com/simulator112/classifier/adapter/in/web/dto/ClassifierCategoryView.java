package com.simulator112.classifier.adapter.in.web.dto;

import java.util.List;

public record ClassifierCategoryView(
        String code,
        String name,
        List<ClassifierEntryView> entries
) {
}
