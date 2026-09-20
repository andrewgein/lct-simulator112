package com.simulator112.classifier.adapter.in.grpc;

import com.simulator112.classifier.domain.model.ClassifierEntry;
import com.simulator112.classifier.domain.model.DispatchService;
import com.simulator112.classifier.domain.model.RoutingDecision;
import com.simulator112.classifier.domain.model.RoutingResult;

final class ClassifierGrpcMapper {

    private ClassifierGrpcMapper() {
    }

    static com.simulator112.classifier.grpc.contract.ClassifierEntry toProto(ClassifierEntry entry) {
        var builder = com.simulator112.classifier.grpc.contract.ClassifierEntry.newBuilder()
                .setId(entry.id().toString())
                .setCode(entry.code())
                .setCategoryCode(entry.categoryCode())
                .setCategoryName(entry.categoryName())
                .setFinalName(entry.finalName())
                .addAllPrimaryServices(entry.primaryServices().stream().map(ClassifierGrpcMapper::toProto).toList());
        setOptionalFields(builder, entry);
        return builder.build();
    }

    static com.simulator112.classifier.grpc.contract.RoutingResult toProto(RoutingResult result) {
        return com.simulator112.classifier.grpc.contract.RoutingResult.newBuilder()
                .setClassifierCode(result.classifierCode())
                .setIncidentTypeName(result.incidentTypeName())
                .putAllFacts(result.facts())
                .addAllDecisions(result.decisions().stream().map(ClassifierGrpcMapper::toProto).toList())
                .build();
    }

    private static com.simulator112.classifier.grpc.contract.DispatchService toProto(
            DispatchService service) {
        return com.simulator112.classifier.grpc.contract.DispatchService.newBuilder()
                .setId(service.id().toString()).setCode(service.code()).setName(service.name()).build();
    }

    private static com.simulator112.classifier.grpc.contract.RoutingDecision toProto(
            RoutingDecision decision) {
        var builder = com.simulator112.classifier.grpc.contract.RoutingDecision.newBuilder()
                .setService(toProto(decision.service()))
                .setRoutingTarget(decision.routingTarget())
                .setResultKind(com.simulator112.classifier.grpc.contract.RoutingResultKind.valueOf(
                        decision.resultKind().name()));
        if (decision.matchedVariant() != null) builder.setMatchedVariant(decision.matchedVariant());
        if (decision.targetTypeName() != null) builder.setTargetTypeName(decision.targetTypeName());
        return builder.build();
    }

    private static void setOptionalFields(
            com.simulator112.classifier.grpc.contract.ClassifierEntry.Builder builder,
            ClassifierEntry entry) {
        if (entry.feature1Code() != null) builder.setFeature1Code(entry.feature1Code());
        if (entry.feature1Name() != null) builder.setFeature1Name(entry.feature1Name());
        if (entry.feature2Code() != null) builder.setFeature2Code(entry.feature2Code());
        if (entry.feature2Name() != null) builder.setFeature2Name(entry.feature2Name());
        if (entry.feature3Code() != null) builder.setFeature3Code(entry.feature3Code());
        if (entry.feature3Name() != null) builder.setFeature3Name(entry.feature3Name());
        if (entry.statisticalGroup() != null) builder.setStatisticalGroup(entry.statisticalGroup());
        if (entry.additionalFeatures() != null) builder.setAdditionalFeatures(entry.additionalFeatures());
        if (entry.ekp35Name() != null) builder.setEkp35Name(entry.ekp35Name());
    }
}
