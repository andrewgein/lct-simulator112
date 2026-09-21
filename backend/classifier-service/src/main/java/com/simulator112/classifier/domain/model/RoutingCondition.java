package com.simulator112.classifier.domain.model;

public record RoutingCondition(
        String factCode,
        RoutingConditionOperator operator,
        String expectedValue) {
}
