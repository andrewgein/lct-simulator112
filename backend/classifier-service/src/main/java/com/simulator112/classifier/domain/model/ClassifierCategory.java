package com.simulator112.classifier.domain.model;

import java.util.List;

public record ClassifierCategory(String code, String name, List<ClassifierEntry> entries) {
}
