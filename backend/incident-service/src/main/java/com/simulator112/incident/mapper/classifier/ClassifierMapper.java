package com.simulator112.incident.mapper.classifier;

import com.simulator112.incident.dto.view.classifier.ClassifierCategoryView;
import com.simulator112.incident.dto.view.classifier.ClassifierEntryView;
import com.simulator112.incident.dto.view.classifier.DispatchServiceView;
import com.simulator112.incident.model.entity.classifier.ClassifierCategoryEntity;
import com.simulator112.incident.model.entity.classifier.ClassifierEntryEntity;
import org.springframework.stereotype.Component;

@Component
public class ClassifierMapper {

    public ClassifierCategoryView toCategoryView(ClassifierCategoryEntity category) {
        return new ClassifierCategoryView(
                category.getCode(),
                category.getName(),
                category.getEntries().stream().map(this::toEntryView).toList()
        );
    }

    public ClassifierEntryView toEntryView(ClassifierEntryEntity entry) {
        return new ClassifierEntryView(
                entry.getId(),
                entry.getCode(),
                entry.getCategory().getCode(),
                entry.getCategory().getName(),
                entry.getFeature1Code(),
                entry.getFeature1Name(),
                entry.getFeature2Code(),
                entry.getFeature2Name(),
                entry.getFeature3Code(),
                entry.getFeature3Name(),
                entry.getStatisticalGroup(),
                entry.getAdditionalFeatures(),
                entry.getFinalName(),
                entry.getEkp35Name(),
                entry.getPrimaryServices().stream()
                        .map(service -> new DispatchServiceView(service.getId(), service.getCode(), service.getName()))
                        .toList()
        );
    }
}
