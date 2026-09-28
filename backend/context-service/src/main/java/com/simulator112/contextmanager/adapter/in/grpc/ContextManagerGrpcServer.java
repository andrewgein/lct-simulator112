package com.simulator112.contextmanager.adapter.in.grpc;

import java.util.UUID;

import com.simulator112.context.grpc.contract.*;
import com.simulator112.contextmanager.application.port.in.ContextUseCase;
import com.simulator112.contextmanager.application.port.in.CallUseCase;
import com.simulator112.contextmanager.application.port.in.LevelProgressUseCase;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import com.google.protobuf.Empty;
import com.simulator112.incident.grpc.contract.CallScenario;
import com.simulator112.incident.grpc.contract.IncidentContext;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GrpcService
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextManagerGrpcServer extends ContextManagerServiceGrpc.ContextManagerServiceImplBase {

    private final ContextUseCase contextService;
    private final CallUseCase dialogService;
    private final LevelProgressUseCase levelProgressService;

    private void onError(Exception e, StreamObserver<?> responseObserver) {
        log.error(e.getMessage());
        responseObserver.onError(Status.INTERNAL
                .withCause(e)
                .withDescription(e.getMessage())
                .asRuntimeException());
    }

    private <T> void onSuccess(T data, StreamObserver<T> responseObserver) {
        responseObserver.onNext(data);
        responseObserver.onCompleted();
    }

    @Override
    public void getIncidentContext(GetIncidentContextRequest request, StreamObserver<IncidentContext> responseObserver) {
        try {
            IncidentContext incidentContext = com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper
                    .toProto(contextService.getIncidentContext(request.getUuid()));
            onSuccess(incidentContext, responseObserver);
        } catch(Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void appendDialogContext(AppendDialogContextRequest request, StreamObserver<Empty> responseObserver) {
        try {
            dialogService.appendDialog(request.getUuid(), request.getCallId(),
                    com.simulator112.contextmanager.adapter.grpc.mapper.DialogContextMapper.toDomain(request.getDialogContext()));
            onSuccess(Empty.getDefaultInstance(), responseObserver);
        } catch(Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getCallTranscript(GetCallTranscriptRequest request, StreamObserver<DialogContext> responseObserver) {
        try {
            onSuccess(com.simulator112.contextmanager.adapter.grpc.mapper.DialogContextMapper.toProto(
                    dialogService.getCallTranscript(request.getContextId(), request.getCallId())), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void clearCallTranscript(ClearCallTranscriptRequest request, StreamObserver<Empty> responseObserver) {
        try {
            dialogService.clearCallTranscript(request.getContextId(), request.getCallId());
            onSuccess(Empty.getDefaultInstance(), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getNextCall(GetNextCallRequest request, StreamObserver<CallScenario> responseObserver) {
        try {
            var nextDomain = dialogService.getNextCall(request.getUuid(), request.getCurrentCallId());
            CallScenario next = nextDomain == null ? null
                    : com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper.toProto(nextDomain);
            if (next == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Звонки для контекста " + request.getUuid() + " закончились")
                        .asRuntimeException());
                return;
            }
            onSuccess(next, responseObserver);
        } catch(Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getCall(GetCallRequest request, StreamObserver<CallScenario> responseObserver) {
        try {
            onSuccess(com.simulator112.contextmanager.adapter.grpc.mapper.IncidentContextMapper.toProto(
                    dialogService.getCall(request.getContextId(), request.getCallId())), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getDialogProgress(GetDialogProgressRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(toProto(dialogService.getDialogProgress(request.getContextId())), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void startCall(StartCallRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(toProto(dialogService.startCall(request.getContextId(), request.getCallId())), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void completeCall(CompleteCallRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(toProto(dialogService.completeCall(request.getContextId(), request.getCallId())), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void disconnectCall(DisconnectCallRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(toProto(dialogService.disconnectCall(request.getContextId(), request.getCallId())), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getLevelProgress(GetLevelProgressRequest request, StreamObserver<LevelProgress> observer) {
        try {
            onSuccess(toProto(levelProgressService.getProgress(UUID.fromString(request.getContextId()))), observer);
        } catch (Exception e) {
            onError(e, observer);
        }
    }

    private DialogProgress toProto(com.simulator112.contextmanager.domain.common.DialogProgress progress) {
        return DialogProgress.newBuilder().setContextId(progress.contextId().toString())
                .setActiveCallId(progress.activeCallId() == null ? "" : progress.activeCallId().toString())
                .setStatus(com.simulator112.context.grpc.contract.DialogProgressStatus.valueOf(progress.status().name()))
                .build();
    }

    private LevelProgress toProto(com.simulator112.contextmanager.domain.common.LevelProgress progress) {
        return com.simulator112.contextmanager.adapter.grpc.mapper.LevelProgressGrpcMapper.toProto(progress);
    }

    // TODO: frontend grpc ??
      /*@Override
      public void appendSolutionContext(AppendSolutionContextRequest request, StreamObserver<Empty> responseObserver) {                     
          try {                                                                                                                                                         
              contextService.appendSolutionContext(request.getUuid(), request.getSolutionContext());
          } catch(Exception e) {
              onError(e, responseObserver);
          }
      }*/

}