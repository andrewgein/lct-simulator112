package com.simulator112.incident.service.classifier;

import com.simulator112.incident.dto.view.classifier.ClassifierCategoryView;
import com.simulator112.incident.mapper.classifier.ClassifierMapper;
import com.simulator112.incident.repository.classifier.ClassifierCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassifierService {

    private final ClassifierCategoryRepository classifierCategoryRepository;
    private final ClassifierMapper classifierMapper;

    @Transactional(readOnly = true)
    public List<ClassifierCategoryView> getClassifier() {
        return classifierCategoryRepository.findAllByOrderByPositionAsc().stream()
                .map(classifierMapper::toCategoryView)
                .toList();
    }
}
