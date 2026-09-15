package com.simulator112.incident.dto.view;

import java.util.List;
import java.util.UUID;

public record ClassifierTypeView(
        UUID id,
        String typeId,
        String name,
        List<ClassifierFieldView> fields,
        List<String> instructions
) {
}
