package com.simulator112.classifier.adapter.in.web.dto;

import java.util.List;

public record ClassifierView(
        List<ClassifierCategoryView> categories,
        List<RoutingFactView> routingFacts
) {
}
