package com.simulator112.classifier.application.port.in;

import com.simulator112.classifier.domain.model.ClassifierCategory;

import java.util.List;

public interface GetClassifierUseCase {
    List<ClassifierCategory> getClassifier();
}
