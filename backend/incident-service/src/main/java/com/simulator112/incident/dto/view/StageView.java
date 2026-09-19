package com.simulator112.incident.dto.view;

import com.simulator112.incident.dto.view.embeddable.ApplicantView;
import com.simulator112.incident.dto.view.classifier.ClassifierEntryView;

import java.util.List;
import java.util.UUID;

public record StageView(
    UUID id,
    String title,
    Integer position,
    ClassifierEntryView classifierEntry,
    String description,
    ApplicantView victim,
    List<DialupView> dialups
) {
}
