package com.simulator112.incident.service;

import com.simulator112.incident.dto.view.ClassifierFieldView;
import com.simulator112.incident.dto.view.ClassifierTypeView;
import com.simulator112.incident.model.entity.TypeEntity;
import com.simulator112.incident.model.enums.ServiceType;
import com.simulator112.incident.repository.TypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassifierService {

    private final TypeRepository typeRepository;

    @Transactional(readOnly = true)
    public Map<String, Map<String, ClassifierTypeView>> getClassifier() {
        Map<String, Map<String, ClassifierTypeView>> classifier = new LinkedHashMap<>();

        typeRepository.findAllByOrderByServiceTypeAscTypeIdAsc()
                .forEach(type -> classifier
                        .computeIfAbsent(toServiceName(type.getServiceType()), key -> new LinkedHashMap<>())
                        .put(type.getTypeId(), toView(type)));

        return classifier;
    }

    private ClassifierTypeView toView(TypeEntity type) {
        return new ClassifierTypeView(
                type.getId(),
                type.getTypeId(),
                type.getTypeName(),
                type.getFields().stream()
                        .map(field -> new ClassifierFieldView(
                                field.getId(),
                                field.getFieldName(),
                                field.getFieldType(),
                                field.isRequired()
                        ))
                        .toList(),
                type.getInstructions().stream()
                        .map(instruction -> instruction.getInstructions())
                        .toList()
        );
    }

    private String toServiceName(ServiceType serviceType) {
        return serviceType.name()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }
}
