package com.simulator112.contextmanager.adapter.out.grpc;

import com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper;
import com.simulator112.contextmanager.application.port.out.IncidentCatalogPort;
import com.simulator112.contextmanager.domain.common.IncidentSnapshot;
import com.simulator112.incident.grpc.contract.GetIncidentRequest;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentServiceGrpcClient implements IncidentCatalogPort {
    private final IncidentGrpcServiceGrpc.IncidentGrpcServiceBlockingStub incidentServiceStub;

    @Override
    public IncidentSnapshot getIncident(UUID incidentId, int position) {
        var incident = incidentServiceStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                .getIncident(GetIncidentRequest.newBuilder().setIncidentId(incidentId.toString()).build());
        return IncidentContextMapper.toDomain(incident, position);
    }
}
