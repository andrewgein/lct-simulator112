package com.simulator112.classifier.application.port.in;

import com.simulator112.classifier.domain.model.ClassifierEntry;

public interface GetClassifierEntryUseCase {
    ClassifierEntry getClassifierEntry(String code);
}
