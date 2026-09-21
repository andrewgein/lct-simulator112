package com.simulator112.classifier.adapter.in.web;

import com.simulator112.classifier.adapter.in.web.dto.*;
import com.simulator112.classifier.domain.model.*;

final class ClassifierWebMapper {

    private ClassifierWebMapper() {
    }

    static ClassifierCategoryView toView(ClassifierCategory category) {
        return new ClassifierCategoryView(
                category.code(), category.name(), category.entries().stream().map(ClassifierWebMapper::toView).toList());
    }

    static ClassifierEntryView toView(ClassifierEntry entry) {
        return new ClassifierEntryView(
                entry.id(), entry.code(), entry.categoryCode(), entry.categoryName(),
                entry.feature1Code(), entry.feature1Name(), entry.feature2Code(), entry.feature2Name(),
                entry.feature3Code(), entry.feature3Name(), entry.statisticalGroup(),
                entry.additionalFeatures(), entry.finalName(), entry.ekp35Name(),
                entry.primaryServices().stream().map(ClassifierWebMapper::toView).toList());
    }

    static RoutingResultView toView(RoutingResult result) {
        return new RoutingResultView(
                result.classifierCode(), result.incidentTypeName(), result.facts(),
                result.decisions().stream().map(ClassifierWebMapper::toView).toList());
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
