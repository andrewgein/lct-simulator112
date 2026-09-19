package com.simulator112.incident.service;

import com.simulator112.incident.model.entity.RoutingVariantConditionEntity;
import com.simulator112.incident.model.entity.RoutingVariantEntity;
import com.simulator112.incident.model.enums.RoutingConditionOperator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingConditionEvaluatorTest {

    private final RoutingConditionEvaluator evaluator = new RoutingConditionEvaluator();

    @Test
    void matchesAllVariantConditions() {
        RoutingVariantEntity variant = variant(
                condition("VICTIM_STATUS", RoutingConditionOperator.EQUALS, "PRESENT"),
                condition("ACCESS_STATUS", RoutingConditionOperator.NOT_EQUALS, "NO_ACCESS")
        );

        boolean matches = evaluator.matches(variant, Map.of(
                "VICTIM_STATUS", "PRESENT",
                "ACCESS_STATUS", "AVAILABLE"
        ));

        assertThat(matches).isTrue();
    }

    @Test
    void doesNotTreatUnknownAsNotEquals() {
        RoutingVariantEntity variant = variant(
                condition("ACCESS_STATUS", RoutingConditionOperator.NOT_EQUALS, "NO_ACCESS")
        );

        assertThat(evaluator.matches(variant, Map.of("ACCESS_STATUS", "UNKNOWN"))).isFalse();
        assertThat(evaluator.matches(variant, Map.of())).isFalse();
    }

    @Test
    void supportsSetAndExistenceOperators() {
        RoutingVariantEntity variant = variant(
                condition("LOCATION_KIND", RoutingConditionOperator.IN, "TUNNEL, METRO"),
                condition("CASUALTY_STATUS", RoutingConditionOperator.EXISTS, null),
                condition("OFFENSE_STATUS", RoutingConditionOperator.NOT_EXISTS, null)
        );

        boolean matches = evaluator.matches(variant, Map.of(
                "LOCATION_KIND", "metro",
                "CASUALTY_STATUS", "NONE"
        ));

        assertThat(matches).isTrue();
    }

    private RoutingVariantEntity variant(RoutingVariantConditionEntity... conditions) {
        return RoutingVariantEntity.builder()
                .conditions(List.of(conditions))
                .build();
    }

    private RoutingVariantConditionEntity condition(
            String factCode,
            RoutingConditionOperator operator,
            String expectedValue
    ) {
        return RoutingVariantConditionEntity.builder()
                .factCode(factCode)
                .operator(operator)
                .expectedValue(expectedValue)
                .build();
    }
}
