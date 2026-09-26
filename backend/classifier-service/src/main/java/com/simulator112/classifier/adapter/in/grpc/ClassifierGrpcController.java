package com.simulator112.classifier.adapter.in.grpc;

import com.simulator112.classifier.application.port.in.GetClassifierEntryUseCase;
import com.simulator112.classifier.application.port.in.GetClassifierUseCase;
import com.simulator112.classifier.application.port.in.ResolveRoutingUseCase;
import com.simulator112.classifier.application.port.in.SearchClassifierEntriesUseCase;
import com.simulator112.classifier.domain.exception.ClassifierEntryNotFoundException;
import com.simulator112.classifier.grpc.contract.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ClassifierGrpcController extends ClassifierServiceGrpc.ClassifierServiceImplBase {

    private final GetClassifierEntryUseCase classifier;
    private final GetClassifierUseCase catalog;
    private final ResolveRoutingUseCase routing;
    private final SearchClassifierEntriesUseCase search;

    @Override
    public void getClassifierEntry(
            GetClassifierEntryRequest request, StreamObserver<ClassifierEntry> observer) {
        try {
            requireCode(request.getClassifierCode());
            observer.onNext(ClassifierGrpcMapper.toProto(
                    classifier.getClassifierEntry(request.getClassifierCode())));
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception));
        }
    }

    @Override
    public void hasDispatchService(HasDispatchServiceRequest request, StreamObserver<HasDispatchServiceResponse> observer) {
        try {
            requireCode(request.getServiceCode());
            observer.onNext(HasDispatchServiceResponse.newBuilder().setExists(catalog.hasService(request.getServiceCode())).build());
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception));
        }
    }

    @Override
    public void searchClassifierEntries(SearchClassifierEntriesRequest request,
                                        StreamObserver<SearchClassifierEntriesResponse> observer) {
        try {
            var response = SearchClassifierEntriesResponse.newBuilder();
            for (var entry : search.search(request.getQuery(), request.getLimit(), request.getIncludedCodesList())) {
                response.addEntries(ClassifierCandidate.newBuilder().setCode(entry.code())
                        .setCategoryName(entry.categoryName()).setFinalName(entry.finalName()).build());
            }
            observer.onNext(response.build());
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception));
        }
    }

    @Override
    public void resolveRouting(ResolveRoutingRequest request, StreamObserver<RoutingResult> observer) {
        try {
            requireCode(request.getClassifierCode());
            observer.onNext(ClassifierGrpcMapper.toProto(
                    routing.resolve(request.getClassifierCode(), request.getFactsMap())));
            observer.onCompleted();
        } catch (RuntimeException exception) {
            observer.onError(toStatus(exception));
        }
    }

    private void requireCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("classifier_code не должен быть пустым");
        }
    }

    private RuntimeException toStatus(RuntimeException exception) {
        if (exception instanceof ClassifierEntryNotFoundException) {
            return Status.NOT_FOUND.withDescription(exception.getMessage()).asRuntimeException();
        }
        if (exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(exception.getMessage()).asRuntimeException();
        }
        log.error("Ошибка classifier-service", exception);
        return Status.INTERNAL.withDescription("Внутренняя ошибка классификатора")
                .withCause(exception).asRuntimeException();
    }
}
