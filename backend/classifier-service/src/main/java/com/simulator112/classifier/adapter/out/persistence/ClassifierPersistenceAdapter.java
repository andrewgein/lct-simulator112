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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClassifierPersistenceAdapter implements ClassifierRepository {

    private final ClassifierCategoryRepository categoryRepository;
    private final ClassifierEntryRepository entryRepository;
    private final RoutingRuleRepository routingRuleRepository;

    @Override
    public List<ClassifierCategory> findAllCategories() {
        Map<UUID, LinkedHashSet<String>> collectedFactCodes = new LinkedHashMap<>();
        routingRuleRepository.findAllFactCodes().forEach(fact -> collectedFactCodes
                .computeIfAbsent(fact.getEntryId(), ignored -> new LinkedHashSet<>())
                .add(fact.getFactCode()));
        Map<UUID, List<String>> factCodes = new LinkedHashMap<>();
        collectedFactCodes.forEach((entryId, codes) -> factCodes.put(entryId, List.copyOf(codes)));
        return categoryRepository.findAllByOrderByPositionAsc().stream()
                .map(category -> ClassifierPersistenceMapper.toDomain(category, factCodes))
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
