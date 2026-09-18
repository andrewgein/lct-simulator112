package com.simulator112.incident.mapper;

import com.simulator112.incident.dto.view.ClassifierCategoryView;
import com.simulator112.incident.dto.view.ClassifierEntryView;
import com.simulator112.incident.dto.view.DispatchServiceView;
import com.simulator112.incident.model.entity.ClassifierCategoryEntity;
import com.simulator112.incident.model.entity.ClassifierEntryEntity;
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
