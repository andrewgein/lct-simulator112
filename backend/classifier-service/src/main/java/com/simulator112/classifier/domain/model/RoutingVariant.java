package com.simulator112.classifier.domain.model;

import java.util.List;

public record RoutingVariant(
        DispatchService dispatchService,
        String routingTarget,
        String headerLevel2,
        String headerLevel3,
        int priority,
        int position,
        List<RoutingCondition> conditions) {
}
