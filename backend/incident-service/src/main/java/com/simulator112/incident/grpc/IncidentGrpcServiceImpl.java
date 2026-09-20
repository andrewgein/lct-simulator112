package com.simulator112.incident.grpc;

import com.simulator112.incident.grpc.contract.GetIncidentContextRequest;
import com.simulator112.incident.grpc.contract.LevelContext;
import com.simulator112.incident.grpc.contract.IncidentGrpcServiceGrpc;
import com.simulator112.incident.model.entity.LevelEntity;
import com.simulator112.incident.repository.LevelRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class IncidentGrpcServiceImpl extends IncidentGrpcServiceGrpc.IncidentGrpcServiceImplBase {

    private final LevelRepository levelRepository;
    private final GrpcIncidentMapper grpcIncidentMapper;

    @Override
    @Transactional(readOnly = true)
    public void getIncidentContext(GetIncidentContextRequest request, StreamObserver<LevelContext> responseObserver) {

        UUID levelId = parseLevelId(request.getLevelId());

        log.debug("ContextManager запросил контекст уровня {}", levelId);

        LevelEntity level = levelRepository.findById(levelId)
                .orElseThrow(() -> Status.NOT_FOUND
                        .withDescription("Уровень с id " + levelId + " не найден")
                        .asRuntimeException());

        responseObserver.onNext(grpcIncidentMapper.toLevelContext(level));
        
        responseObserver.onCompleted();
    }

    private UUID parseLevelId(String levelId) {

        try {
            return UUID.fromString(levelId);

        } catch (IllegalArgumentException e) {
            throw Status.INVALID_ARGUMENT
                    .withDescription("Некорректный формат level_id: " + levelId)
                    .asRuntimeException();
        }
    }
}
