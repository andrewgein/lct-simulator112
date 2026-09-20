package com.simulator112.classifier.application.service;

import com.simulator112.classifier.application.port.in.GetClassifierEntryUseCase;
import com.simulator112.classifier.application.port.in.GetClassifierUseCase;
import com.simulator112.classifier.application.port.out.ClassifierRepository;
import com.simulator112.classifier.domain.exception.ClassifierEntryNotFoundException;
import com.simulator112.classifier.domain.model.ClassifierCategory;
import com.simulator112.classifier.domain.model.ClassifierEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassifierApplicationService
        implements GetClassifierUseCase, GetClassifierEntryUseCase {

    private final ClassifierRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassifierCategory> getClassifier() {
        return repository.findAllCategories();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassifierEntry getClassifierEntry(String code) {
        return repository.findEntryByCode(code)
                .orElseThrow(() -> new ClassifierEntryNotFoundException(code));
    }
}
