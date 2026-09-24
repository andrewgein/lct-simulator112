package com.simulator112.classifier.adapter.out.persistence;

import com.simulator112.classifier.adapter.out.persistence.entity.ClassifierCategoryEntity;
import com.simulator112.classifier.adapter.out.persistence.entity.ClassifierEntryEntity;
import com.simulator112.classifier.adapter.out.persistence.entity.RoutingRuleEntity;
import com.simulator112.classifier.domain.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ClassifierPersistenceMapper {

    private ClassifierPersistenceMapper() {
    }

    static ClassifierCategory toDomain(ClassifierCategoryEntity entity, Map<UUID, List<String>> routingFactCodes) {
        return new ClassifierCategory(
                entity.getCode(),
                entity.getName(),
                entity.getEntries().stream()
                        .map(entry -> toDomain(entry, routingFactCodes.getOrDefault(entry.getId(), List.of())))
                        .toList());
    }

    static ClassifierEntry toDomain(ClassifierEntryEntity entity) {
        return toDomain(entity, List.of());
    }

    private static ClassifierEntry toDomain(ClassifierEntryEntity entity, List<String> routingFactCodes) {
        return new ClassifierEntry(
                entity.getId(),
                entity.getCode(),
                entity.getCategory().getCode(),
                entity.getCategory().getName(),
                entity.getFeature1Code(),
                entity.getFeature1Name(),
                entity.getFeature2Code(),
                entity.getFeature2Name(),
                entity.getFeature3Code(),
                entity.getFeature3Name(),
                entity.getStatisticalGroup(),
                entity.getAdditionalFeatures(),
                entity.getFinalName(),
                entity.getEkp35Name(),
                entity.getPrimaryServices().stream()
                        .map(service -> new DispatchService(service.getId(), service.getCode(), service.getName()))
                        .toList(),
                routingFactCodes);
    }

    static RoutingRule toDomain(RoutingRuleEntity entity) {
        var variant = entity.getVariant();
        var service = variant.getDispatchService();
        return new RoutingRule(
                new RoutingVariant(
                        new DispatchService(service.getId(), service.getCode(), service.getName()),
                        variant.getRoutingTarget(),
                        variant.getHeaderLevel2(),
                        variant.getHeaderLevel3(),
                        variant.getPriority(),
                        variant.getPosition(),
                        variant.getConditions().stream()
                                .map(condition -> new RoutingCondition(
                                        condition.getFactCode(), condition.getOperator(), condition.getExpectedValue()))
                                .toList()),
                entity.getResultKind(),
                entity.getTargetTypeName());
    }
}
