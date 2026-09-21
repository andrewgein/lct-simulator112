package com.simulator112.classifier.application.service;

import com.simulator112.classifier.application.port.in.ResolveRoutingUseCase;
import com.simulator112.classifier.application.port.out.ClassifierRepository;
import com.simulator112.classifier.domain.exception.ClassifierEntryNotFoundException;
import com.simulator112.classifier.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RoutingApplicationService implements ResolveRoutingUseCase {

    private static final Comparator<RoutingRule> RULE_PRIORITY = Comparator
            .comparing((RoutingRule rule) -> rule.variant().priority())
            .reversed()
            .thenComparing(rule -> rule.variant().position());

    private final ClassifierRepository repository;
    private final RoutingConditionEvaluator conditionEvaluator;

    @Override
    @Transactional(readOnly = true)
    public RoutingResult resolve(String classifierCode, Map<String, String> sourceFacts) {
        ClassifierEntry entry = repository.findEntryByCode(classifierCode)
                .orElseThrow(() -> new ClassifierEntryNotFoundException(classifierCode));
        Map<String, String> facts = normalizeFacts(sourceFacts);
        List<RoutingDecision> decisions = groupByTarget(repository.findRoutingRules(classifierCode))
                .values().stream()
                .map(rules -> selectRule(rules, facts))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(rule -> rule.variant().position()))
                .map(this::toDecision)
                .toList();
        return new RoutingResult(entry.code(), entry.finalName(), facts, decisions);
    }

    private Map<RoutingTarget, List<RoutingRule>> groupByTarget(List<RoutingRule> rules) {
        Map<RoutingTarget, List<RoutingRule>> result = new LinkedHashMap<>();
        for (RoutingRule rule : rules) {
            RoutingVariant variant = rule.variant();
            RoutingTarget target = new RoutingTarget(
                    variant.dispatchService().code(), variant.routingTarget());
            result.computeIfAbsent(target, ignored -> new ArrayList<>()).add(rule);
        }
        return result;
    }

    private Optional<RoutingRule> selectRule(List<RoutingRule> rules, Map<String, String> facts) {
        return rules.stream()
                .filter(rule -> conditionEvaluator.matches(rule.variant(), facts))
                .min(RULE_PRIORITY);
    }

    private RoutingDecision toDecision(RoutingRule rule) {
        RoutingVariant variant = rule.variant();
        return new RoutingDecision(
                variant.dispatchService(),
                variant.routingTarget(),
                routingVariant(variant),
                rule.resultKind(),
                rule.targetTypeName());
    }

    private String routingVariant(RoutingVariant variant) {
        String level3 = clean(variant.headerLevel3());
        if (level3 != null) {
            return level3;
        }
        String level2 = clean(variant.headerLevel2());
        return level2 != null && level2.equals(variant.routingTarget()) ? null : level2;
    }

    private Map<String, String> normalizeFacts(Map<String, String> source) {
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            String normalizedValue = normalize(value);
            String previous = result.putIfAbsent(normalizedKey, normalizedValue);
            if (previous != null && !previous.equals(normalizedValue)) {
                throw new IllegalArgumentException(
                        "Факт " + normalizedKey + " передан с разными значениями");
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private String normalize(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Код и значение факта не должны быть null");
        }
        return value.strip().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private record RoutingTarget(String serviceCode, String name) {
    }
}
