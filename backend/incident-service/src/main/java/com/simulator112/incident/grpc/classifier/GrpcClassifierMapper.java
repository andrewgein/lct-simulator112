package com.simulator112.incident.grpc.classifier;

import com.simulator112.incident.dto.request.classifier.ResolveRoutingRequest;
import com.simulator112.incident.dto.view.classifier.RoutingDecisionView;
import com.simulator112.incident.dto.view.classifier.RoutingResultView;
import com.simulator112.incident.grpc.contract.ClassifierEntry;
import com.simulator112.incident.grpc.contract.DispatchService;
import com.simulator112.incident.grpc.contract.RoutingDecision;
import com.simulator112.incident.grpc.contract.RoutingResult;
import com.simulator112.incident.model.entity.classifier.ClassifierEntryEntity;
import org.springframework.stereotype.Component;

@Component
public class GrpcClassifierMapper {

    public ResolveRoutingRequest toRequest(
            com.simulator112.incident.grpc.contract.ResolveRoutingRequest request
    ) {
        return new ResolveRoutingRequest(request.getFactsMap());
    }

    public ClassifierEntry toClassifierEntry(ClassifierEntryEntity entry) {
        return ClassifierEntry.newBuilder()
                .setId(entry.getId().toString())
                .setCode(entry.getCode())
                .setName(entry.getFinalName())
                .addAllPrimaryServices(entry.getPrimaryServices().stream()
                        .map(service -> DispatchService.newBuilder()
                                .setId(service.getId().toString())
                                .setCode(service.getCode())
                                .setName(service.getName())
                                .build())
                        .toList())
                .build();
    }

    public RoutingResult toRoutingResult(RoutingResultView view) {
        return RoutingResult.newBuilder()
                .setClassifierCode(view.classifierCode())
                .setIncidentTypeName(view.incidentTypeName())
                .putAllFacts(view.facts())
                .addAllDecisions(view.decisions().stream().map(this::toRoutingDecision).toList())
                .build();
    }

    private RoutingDecision toRoutingDecision(RoutingDecisionView view) {
        RoutingDecision.Builder builder = RoutingDecision.newBuilder()
                .setService(DispatchService.newBuilder()
                        .setId(view.service().id().toString())
                        .setCode(view.service().code())
                        .setName(view.service().name())
                        .build())
                .setRoutingTarget(view.routingTarget())
                .setResultKind(com.simulator112.incident.grpc.contract.RoutingResultKind.valueOf(
                        view.resultKind().name()
                ));
        if (view.matchedVariant() != null) {
            builder.setMatchedVariant(view.matchedVariant());
        }
        if (view.targetTypeName() != null) {
            builder.setTargetTypeName(view.targetTypeName());
        }
        return builder.build();
    }
}
