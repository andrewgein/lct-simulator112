package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.*;
import com.simulator112.classifier.domain.model.*;

import java.util.LinkedHashSet;
import java.util.List;

final class ClassifierWebMapper {

    private ClassifierWebMapper() {
    }

    static ClassifierView toView(List<ClassifierCategory> categories) {
        var factCodes = categories.stream()
                .flatMap(category -> category.entries().stream())
                .flatMap(entry -> entry.routingFactCodes().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new ClassifierView(
                categories.stream().map(ClassifierWebMapper::toView).toList(),
                RoutingFactCatalog.definitions(factCodes));
    }

    static ClassifierCategoryView toView(ClassifierCategory category) {
        return new ClassifierCategoryView(
                category.code(), category.name(), category.entries().stream().map(ClassifierWebMapper::toView).toList());
    }

    static ClassifierEntryView toView(ClassifierEntry entry) {
        return new ClassifierEntryView(
                entry.id(), entry.code(), features(entry), entry.statisticalGroup(),
                entry.additionalFeatures(), entry.finalName(), entry.ekp35Name(),
                entry.primaryServices().stream().map(ClassifierWebMapper::toView).toList(),
                entry.routingFactCodes());
    }

    static RoutingResultView toView(RoutingResult result) {
        return new RoutingResultView(
                result.classifierCode(), result.incidentTypeName(), result.facts(),
                result.decisions().stream().map(ClassifierWebMapper::toView).toList());
    }

    private static List<ClassifierFeatureView> features(ClassifierEntry entry) {
        var features = new java.util.ArrayList<ClassifierFeatureView>();
        addFeature(features, 1, entry.feature1Code(), entry.feature1Name());
        addFeature(features, 2, entry.feature2Code(), entry.feature2Name());
        addFeature(features, 3, entry.feature3Code(), entry.feature3Name());
        return List.copyOf(features);
    }

    private static void addFeature(List<ClassifierFeatureView> features, int level, String code, String name) {
        if ((code != null && !code.isBlank()) || (name != null && !name.isBlank())) {
            features.add(new ClassifierFeatureView(level, code, name));
        }
    }

    private static DispatchServiceView toView(DispatchService service) {
        return new DispatchServiceView(service.id(), service.code(), service.name());
    }

    private static RoutingDecisionView toView(RoutingDecision decision) {
        return new RoutingDecisionView(
                toView(decision.service()), decision.routingTarget(), decision.matchedVariant(),
                decision.resultKind(), decision.targetTypeName());
    }
}
