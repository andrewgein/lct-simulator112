package com.simulator112.incident.client.classifier;

import com.simulator112.classifier.grpc.contract.ClassifierServiceGrpc;
import com.simulator112.classifier.grpc.contract.GetClassifierEntryRequest;
import com.simulator112.incident.dto.view.classifier.ClassifierEntryView;
import com.simulator112.incident.dto.view.classifier.DispatchServiceView;
import com.simulator112.incident.exception.classifier.ClassifierEntryNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ClassifierGrpcClient implements ClassifierClient {

  private final ClassifierServiceGrpc.ClassifierServiceBlockingStub stub;

  @Override
  public ClassifierEntryView getEntry(String classifierCode) {
    try {
      var entry = stub.getClassifierEntry(GetClassifierEntryRequest.newBuilder()
          .setClassifierCode(classifierCode).build());
      return new ClassifierEntryView(
          java.util.UUID.fromString(entry.getId()),
          entry.getCode(),
          entry.getCategoryCode(),
          entry.getCategoryName(),
          entry.hasFeature1Code() ? entry.getFeature1Code() : null,
          entry.hasFeature1Name() ? entry.getFeature1Name() : null,
          entry.hasFeature2Code() ? entry.getFeature2Code() : null,
          entry.hasFeature2Name() ? entry.getFeature2Name() : null,
          entry.hasFeature3Code() ? entry.getFeature3Code() : null,
          entry.hasFeature3Name() ? entry.getFeature3Name() : null,
          entry.hasStatisticalGroup() ? entry.getStatisticalGroup() : null,
          entry.hasAdditionalFeatures() ? entry.getAdditionalFeatures() : null,
          entry.getFinalName(),
          entry.hasEkp35Name() ? entry.getEkp35Name() : null,
          entry.getPrimaryServicesList().stream()
              .map(service -> new DispatchServiceView(
                  java.util.UUID.fromString(service.getId()), service.getCode(), service.getName()))
              .toList());
    } catch (StatusRuntimeException exception) {
      if (exception.getStatus().getCode() == Status.Code.NOT_FOUND) {
        throw new ClassifierEntryNotFoundException(classifierCode);
      }
      throw exception;
    }
  }
}
