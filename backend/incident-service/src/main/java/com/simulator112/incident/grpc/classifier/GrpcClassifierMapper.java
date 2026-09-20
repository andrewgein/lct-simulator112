package com.simulator112.incident.grpc.classifier;

import com.simulator112.classifier.grpc.contract.ClassifierEntry;
import com.simulator112.classifier.grpc.contract.DispatchService;
import com.simulator112.incident.dto.view.classifier.ClassifierEntryView;
import org.springframework.stereotype.Component;

@Component
public class GrpcClassifierMapper {

  public ClassifierEntry toClassifierEntry(ClassifierEntryView entry) {
    var builder = ClassifierEntry.newBuilder()
        .setId(entry.id().toString())
        .setCode(entry.code())
        .setCategoryCode(entry.categoryCode())
        .setCategoryName(entry.categoryName())
        .setFinalName(entry.finalName())
        .addAllPrimaryServices(entry.primaryServices().stream()
            .map(service -> DispatchService.newBuilder()
                .setId(service.id().toString())
                .setCode(service.code())
                .setName(service.name())
                .build())
            .toList());
    if (entry.feature1Code() != null) builder.setFeature1Code(entry.feature1Code());
    if (entry.feature1Name() != null) builder.setFeature1Name(entry.feature1Name());
    if (entry.feature2Code() != null) builder.setFeature2Code(entry.feature2Code());
    if (entry.feature2Name() != null) builder.setFeature2Name(entry.feature2Name());
    if (entry.feature3Code() != null) builder.setFeature3Code(entry.feature3Code());
    if (entry.feature3Name() != null) builder.setFeature3Name(entry.feature3Name());
    if (entry.statisticalGroup() != null) builder.setStatisticalGroup(entry.statisticalGroup());
    if (entry.additionalFeatures() != null) builder.setAdditionalFeatures(entry.additionalFeatures());
    if (entry.ekp35Name() != null) builder.setEkp35Name(entry.ekp35Name());
    return builder.build();
  }
}
