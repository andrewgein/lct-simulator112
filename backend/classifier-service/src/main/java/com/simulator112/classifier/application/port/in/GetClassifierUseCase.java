package com.simulator112.classifier.application.port.in;

import com.simulator112.classifier.domain.model.ClassifierCategory;
import com.simulator112.classifier.domain.model.DispatchService;

import java.util.List;

public interface GetClassifierUseCase {
    List<ClassifierCategory> getClassifier();

    List<DispatchService> getServices();

    boolean hasService(String code);
}
