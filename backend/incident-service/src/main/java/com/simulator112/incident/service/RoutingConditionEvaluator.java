package com.simulator112.incident.service;

import com.simulator112.incident.model.entity.RoutingVariantConditionEntity;
import com.simulator112.incident.model.entity.RoutingVariantEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class RoutingConditionEvaluator {

    public boolean matches(RoutingVariantEntity variant, Map<String, String> facts) {
        return variant.getConditions().stream().allMatch(condition -> matches(condition, facts));
    }

    private boolean matches(RoutingVariantConditionEntity condition, Map<String, String> facts) {
        String sourceValue = facts.get(normalize(condition.getFactCode()));
        String actualValue = normalize(sourceValue);
        boolean exists = sourceValue != null && !actualValue.isBlank() && !actualValue.equals("UNKNOWN");
        String expectedValue = normalize(condition.getExpectedValue());

        return switch (condition.getOperator()) {
            case EQUALS -> exists && actualValue.equals(expectedValue);
            case NOT_EQUALS -> exists && !actualValue.equals(expectedValue);
            case IN -> exists && values(expectedValue).anyMatch(actualValue::equals);
            case NOT_IN -> exists && values(expectedValue).noneMatch(actualValue::equals);
            case EXISTS -> exists;
            case NOT_EXISTS -> !exists;
        };
    }

    private Stream<String> values(String value) {
        return Arrays.stream(value.split("[,;|]"))
                .map(this::normalize)
                .filter(item -> !item.isBlank());
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }
}
