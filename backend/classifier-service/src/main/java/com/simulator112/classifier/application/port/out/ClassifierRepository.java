package com.simulator112.classifier.application.port.out;

import com.simulator112.classifier.domain.model.ClassifierCategory;
import com.simulator112.classifier.domain.model.ClassifierEntry;
import com.simulator112.classifier.domain.model.RoutingRule;

import java.util.List;
import java.util.Optional;

public interface ClassifierRepository {
    List<ClassifierCategory> findAllCategories();

    Optional<ClassifierEntry> findEntryByCode(String code);

    List<RoutingRule> findRoutingRules(String classifierCode);
}
