package com.simulator112.incident.domain.dds;

import com.simulator112.incident.domain.common.CallScenario;
import com.simulator112.incident.domain.common.IncidentStage;
import com.simulator112.incident.domain.common.IncidentStatus;

import java.util.List;
import java.util.UUID;

public record DdsStage(
        UUID id,
        String title,
        String description,
        DdsStageType type,
        int timeLimitSeconds,
        List<CallScenario> calls,
        String expectedComment,
        IncidentStatus actualStatus,
        List<DdsCompletionTrigger> completionTriggers,
        boolean failOnTimeout) implements IncidentStage {
    public DdsStage(UUID id, String title, String description, DdsStageType type,
                    int timeLimitSeconds, List<CallScenario> calls, String expectedComment, IncidentStatus actualStatus,
                    List<DdsCompletionTrigger> completionTriggers) {
        this(id, title, description, type, timeLimitSeconds, calls, expectedComment, actualStatus, completionTriggers, false);
    }

    public DdsStage(UUID id, String title, String description, DdsStageType type,
                    int timeLimitSeconds, List<CallScenario> calls, String expectedComment, IncidentStatus actualStatus) {
        this(id, title, description, type, timeLimitSeconds, calls, expectedComment, actualStatus, type == DdsStageType.ASSIGN_BRIGADE
                ? List.of(DdsCompletionTrigger.TIME, DdsCompletionTrigger.STATUS) : List.of(DdsCompletionTrigger.TIME));
    }
    public DdsStage(UUID id, String title, String description, DdsStageType type,
                    int timeLimitSeconds, List<CallScenario> calls, String expectedComment) {
        this(id, title, description, type, timeLimitSeconds, calls, expectedComment, null);
    }

    public DdsStage(UUID id, String title, String description, DdsStageType type,
                    int timeLimitSeconds, List<CallScenario> calls) {
        this(id, title, description, type, timeLimitSeconds, calls, null, null);
    }

    public DdsStage {
        calls = calls == null ? List.of() : List.copyOf(calls);
        completionTriggers = completionTriggers == null ? (type == DdsStageType.ASSIGN_BRIGADE
                ? List.of(DdsCompletionTrigger.TIME, DdsCompletionTrigger.STATUS) : List.of(DdsCompletionTrigger.TIME))
                : List.copyOf(completionTriggers);
        if (completionTriggers.isEmpty() || completionTriggers.stream().distinct().count() != completionTriggers.size()) {
            throw new IllegalArgumentException("Выберите неповторяющиеся условия завершения этапа ДДС");
        }
        if (failOnTimeout && !completionTriggers.contains(DdsCompletionTrigger.TIME)) {
            throw new IllegalArgumentException("Завершение с ошибкой по времени доступно только при выбранном условии «Время»");
        }
        if (completionTriggers.contains(DdsCompletionTrigger.CALLS) && calls.isEmpty()) {
            throw new IllegalArgumentException("Для завершения этапа по звонкам добавьте звонок");
        }
        if (completionTriggers.contains(DdsCompletionTrigger.STATUS) && (type == DdsStageType.ASSIGN_BRIGADE
                ? actualStatus != null && actualStatus != IncidentStatus.ACCEPTED
                : actualStatus == null || !List.of("ACCEPTED", "RESPONSE_STARTED", "ARRIVED", "WORK_IN_PROGRESS", "WORK_COMPLETED")
                        .contains(actualStatus.name()))) {
            throw new IllegalArgumentException("Для завершения этапа по статусу укажите следующий статус реагирования");
        }
        if (type == null) {
            throw new IllegalArgumentException("Тип этапа ДДС обязателен");
        }
        if (timeLimitSeconds <= 0) {
            throw new IllegalArgumentException("Ограничение времени этапа ДДС должно быть положительным");
        }
    }
}
