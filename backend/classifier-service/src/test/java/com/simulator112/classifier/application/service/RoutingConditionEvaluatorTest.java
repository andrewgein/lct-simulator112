package com.simulator112.classifier.application.service;

import com.simulator112.classifier.domain.model.RoutingCondition;
import com.simulator112.classifier.domain.model.RoutingConditionOperator;
import com.simulator112.classifier.domain.model.RoutingVariant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingConditionEvaluatorTest {

    private final RoutingConditionEvaluator evaluator = new RoutingConditionEvaluator();

    @Test
    void matchesAllVariantConditions() {
        RoutingVariant variant = variant(
                condition("VICTIM_STATUS", RoutingConditionOperator.EQUALS, "PRESENT"),
                condition("ACCESS_STATUS", RoutingConditionOperator.NOT_EQUALS, "NO_ACCESS"));

        assertThat(evaluator.matches(variant, Map.of(
                "VICTIM_STATUS", "PRESENT", "ACCESS_STATUS", "AVAILABLE"))).isTrue();
    }

    @Test
    void doesNotTreatUnknownAsNotEquals() {
        RoutingVariant variant = variant(
                condition("ACCESS_STATUS", RoutingConditionOperator.NOT_EQUALS, "NO_ACCESS"));

        assertThat(evaluator.matches(variant, Map.of("ACCESS_STATUS", "UNKNOWN"))).isFalse();
        assertThat(evaluator.matches(variant, Map.of())).isFalse();
    }

    @Test
    void supportsSetAndExistenceOperators() {
        RoutingVariant variant = variant(
                condition("LOCATION_KIND", RoutingConditionOperator.IN, "TUNNEL, METRO"),
                condition("CASUALTY_STATUS", RoutingConditionOperator.EXISTS, null),
                condition("OFFENSE_STATUS", RoutingConditionOperator.NOT_EXISTS, null));

        assertThat(evaluator.matches(variant, Map.of(
                "LOCATION_KIND", "metro", "CASUALTY_STATUS", "NONE"))).isTrue();
    }

    private RoutingVariant variant(RoutingCondition... conditions) {
        return new RoutingVariant(null, null, null, null, 0, 0, List.of(conditions));
    }

    private RoutingCondition condition(
            String factCode, RoutingConditionOperator operator, String expectedValue) {
        return new RoutingCondition(factCode, operator, expectedValue);
    }
}
