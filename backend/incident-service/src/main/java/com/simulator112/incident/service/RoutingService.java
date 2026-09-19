package com.simulator112.incident.service;

import com.simulator112.incident.dto.request.ResolveRoutingRequest;
import com.simulator112.incident.dto.view.DispatchServiceView;
import com.simulator112.incident.dto.view.RoutingDecisionView;
import com.simulator112.incident.dto.view.RoutingResultView;
import com.simulator112.incident.exception.ClassifierEntryNotFoundException;
import com.simulator112.incident.model.entity.ClassifierEntryEntity;
import com.simulator112.incident.model.entity.DispatchServiceEntity;
import com.simulator112.incident.model.entity.RoutingRuleEntity;
import com.simulator112.incident.model.entity.RoutingVariantEntity;
import com.simulator112.incident.repository.ClassifierEntryRepository;
import com.simulator112.incident.repository.RoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoutingService {

    private static final Comparator<RoutingRuleEntity> RULE_PRIORITY = Comparator
            .comparing((RoutingRuleEntity rule) -> rule.getVariant().getPriority())
            .reversed()
            .thenComparing(rule -> rule.getVariant().getPosition());

    private final ClassifierEntryRepository classifierEntryRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final RoutingConditionEvaluator conditionEvaluator;

    @Transactional(readOnly = true)
    public RoutingResultView resolve(String classifierCode, ResolveRoutingRequest request) {
        ClassifierEntryEntity classifierEntry = classifierEntryRepository.findByCode(classifierCode)
                .orElseThrow(() -> new ClassifierEntryNotFoundException(classifierCode));
        Map<String, String> facts = normalizeFacts(request.facts());
        Map<RoutingTarget, List<RoutingRuleEntity>> rulesByTarget = groupByTarget(
                routingRuleRepository.findAllForRouting(classifierEntry.getId())
        );
        List<RoutingDecisionView> decisions = rulesByTarget.values().stream()
                .map(rules -> selectRule(rules, facts))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(rule -> rule.getVariant().getPosition()))
                .map(this::toView)
                .toList();

        return new RoutingResultView(
                classifierEntry.getCode(),
                classifierEntry.getFinalName(),
                facts,
                decisions
        );
    }

    private Map<RoutingTarget, List<RoutingRuleEntity>> groupByTarget(List<RoutingRuleEntity> rules) {
        Map<RoutingTarget, List<RoutingRuleEntity>> result = new LinkedHashMap<>();
        for (RoutingRuleEntity rule : rules) {
            RoutingVariantEntity variant = rule.getVariant();
            RoutingTarget target = new RoutingTarget(
                    variant.getDispatchService().getCode(),
                    variant.getRoutingTarget()
            );
            result.computeIfAbsent(target, key -> new ArrayList<>()).add(rule);
        }
        return result;
    }

    private Optional<RoutingRuleEntity> selectRule(
            List<RoutingRuleEntity> rules,
            Map<String, String> facts
    ) {
        return rules.stream()
                .filter(rule -> conditionEvaluator.matches(rule.getVariant(), facts))
                .min(RULE_PRIORITY);
    }

    private RoutingDecisionView toView(RoutingRuleEntity rule) {
        RoutingVariantEntity variant = rule.getVariant();
        DispatchServiceEntity service = variant.getDispatchService();
        return new RoutingDecisionView(
                new DispatchServiceView(service.getId(), service.getCode(), service.getName()),
                variant.getRoutingTarget(),
                routingVariant(variant),
                rule.getResultKind(),
                rule.getTargetTypeName()
        );
    }

    private String routingVariant(RoutingVariantEntity variant) {
        String level3 = clean(variant.getHeaderLevel3());
        if (level3 != null) {
            return level3;
        }
        String level2 = clean(variant.getHeaderLevel2());
        return level2 != null && level2.equals(variant.getRoutingTarget()) ? null : level2;
    }

    private Map<String, String> normalizeFacts(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            String normalizedValue = normalize(value);
            String previousValue = result.putIfAbsent(normalizedKey, normalizedValue);
            if (previousValue != null && !previousValue.equals(normalizedValue)) {
                throw new IllegalArgumentException("Факт " + normalizedKey + " передан с разными значениями");
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private String normalize(String value) {
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    private record RoutingTarget(String serviceCode, String name) {
    }
}
