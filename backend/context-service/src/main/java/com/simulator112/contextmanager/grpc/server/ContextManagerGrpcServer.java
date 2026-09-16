package com.simulator112.contextmanager.grpc.server;

import java.util.UUID;

import com.simulator112.context.grpc.contract.*;
import com.simulator112.contextmanager.service.ContextService;
import com.simulator112.contextmanager.service.DialogService;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.stereotype.Component;

import com.google.protobuf.Empty;
import com.simulator112.incident.grpc.contract.DialupContext;
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

    private final ContextService contextService;
    private final DialogService dialogService;

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
            IncidentContext incidentContext = contextService.getIncidentContext(request.getUuid());
            onSuccess(incidentContext, responseObserver);
        } catch(Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void appendDialogContext(AppendDialogContextRequest request, StreamObserver<Empty> responseObserver) {
        try {
            dialogService.appendDialogContext(request.getUuid(), request.getDialogContext());
            onSuccess(Empty.getDefaultInstance(), responseObserver);
        } catch(Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getNextDialup(GetNextDialupRequest request, StreamObserver<DialupContext> responseObserver) {
        try {
            DialupContext next = dialogService.getNextDialup(request.getUuid(), request.getCurrentDialupId());
            if (next == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("Диалапы для контекста " + request.getUuid() + " закончились")
                        .asRuntimeException());
                return;
            }
            onSuccess(next, responseObserver);
        } catch(Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getDialup(GetDialupRequest request, StreamObserver<DialupContext> responseObserver) {
        try {
            onSuccess(dialogService.getDialup(request.getContextId(), request.getDialupId()), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void getDialogProgress(GetDialogProgressRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(dialogService.getDialogProgress(request.getContextId()), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void startDialup(StartDialupRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(dialogService.startDialup(request.getContextId(), request.getDialupId()), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void completeDialup(CompleteDialupRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(dialogService.completeDialup(request.getContextId(), request.getDialupId()), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
    }

    @Override
    public void disconnectDialup(DisconnectDialupRequest request, StreamObserver<DialogProgress> responseObserver) {
        try {
            onSuccess(dialogService.disconnectDialup(request.getContextId(), request.getDialupId()), responseObserver);
        } catch (Exception e) {
            onError(e, responseObserver);
        }
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