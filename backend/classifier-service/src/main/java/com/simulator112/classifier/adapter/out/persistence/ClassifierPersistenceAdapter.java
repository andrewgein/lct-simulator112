package com.simulator112.classifier.adapter.out.persistence;

import com.simulator112.classifier.adapter.out.persistence.repository.ClassifierCategoryRepository;
import com.simulator112.classifier.adapter.out.persistence.repository.ClassifierEntryRepository;
import com.simulator112.classifier.adapter.out.persistence.repository.RoutingRuleRepository;
import com.simulator112.classifier.application.port.out.ClassifierRepository;
import com.simulator112.classifier.domain.model.ClassifierCategory;
import com.simulator112.classifier.domain.model.ClassifierEntry;
import com.simulator112.classifier.domain.model.RoutingRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClassifierPersistenceAdapter implements ClassifierRepository {

    private final ClassifierCategoryRepository categoryRepository;
    private final ClassifierEntryRepository entryRepository;
    private final RoutingRuleRepository routingRuleRepository;

    @Override
    public List<ClassifierCategory> findAllCategories() {
        return categoryRepository.findAllByOrderByPositionAsc().stream()
                .map(ClassifierPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ClassifierEntry> findEntryByCode(String code) {
        return entryRepository.findByCode(code).map(ClassifierPersistenceMapper::toDomain);
    }

    @Override
    public List<RoutingRule> findRoutingRules(String classifierCode) {
        return entryRepository.findByCode(classifierCode)
                .map(entry -> routingRuleRepository.findAllForRouting(entry.getId()).stream()
                        .map(ClassifierPersistenceMapper::toDomain)
                        .toList())
                .orElseGet(List::of);
    }
}
