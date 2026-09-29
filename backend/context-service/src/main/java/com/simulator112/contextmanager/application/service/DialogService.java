package com.simulator112.contextmanager.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.contextmanager.application.port.in.CallUseCase;
import com.simulator112.contextmanager.application.port.out.ContextStore;
import com.simulator112.contextmanager.domain.common.TrainingContext;
import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.DialogProgress;
import com.simulator112.contextmanager.domain.common.DialogTranscript;
import com.simulator112.contextmanager.domain.common.DialogProgressStatus;
import com.simulator112.contextmanager.domain.common.Phrase;
import com.simulator112.contextmanager.domain.common.CallStatus;
import com.simulator112.contextmanager.domain.common.ContextStatus;
import com.simulator112.contextmanager.domain.common.ExecutionMode;
import com.simulator112.contextmanager.domain.common.IncidentProgressStatus;
import com.simulator112.contextmanager.domain.common.IncidentTargetType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DialogService implements CallUseCase {
    private final ContextStore contextStore;

    @Transactional
    public void appendDialog(String id, String callId, DialogTranscript dialog) {
        UUID requestedCallId = parseUuid(callId);
        List<Phrase> tagged = dialog.phrases().stream()
                .map(phrase -> new Phrase(phrase.speaker(), phrase.text(), requestedCallId))
                .toList();
        TrainingContext context = find(id);
        if (context.getDialog() == null) {
            context.setDialog(new DialogTranscript(tagged));
        } else {
            context.getDialog().phrases().addAll(tagged);
        }
        contextStore.save(context);
    }

    @Transactional(readOnly = true)
    public DialogTranscript getCallTranscript(String id, String callId) {
        UUID requestedCallId = parseUuid(callId);
        TrainingContext context = find(id);
        if (context.getDialog() == null) {
            return new DialogTranscript(List.of());
        }
        return new DialogTranscript(context.getDialog().phrases().stream()
                .filter(phrase -> requestedCallId.equals(phrase.callId()))
                .toList());
    }

    @Transactional
    public void clearCallTranscript(String id, String callId) {
        UUID requestedCallId = parseUuid(callId);
        TrainingContext context = find(id);
        if (context.getDialog() == null) {
            return;
        }
        context.getDialog().phrases().removeIf(phrase -> requestedCallId.equals(phrase.callId()));
        contextStore.save(context);
    }

    @Transactional(readOnly = true)
    public DialogProgress getDialogProgress(String id) {
        return toDialogProgress(find(id));
    }

    @Transactional
    public DialogProgress startCall(String id, String callId) {
        TrainingContext context = find(id);
        UUID requestedCallId = parseUuid(callId);
        boolean resumingDds = context.getTargetType() == IncidentTargetType.DDS
                && requestedCallId.equals(context.getActiveCallId())
                && context.getDialogStatus() == DialogProgressStatus.DISCONNECTED;
        CallSnapshot call = resumingDds ? findCall(context, requestedCallId) : requireCall(context, requestedCallId);
        if (!resumingDds && incidentForCall(context, requestedCallId).getStatus() != IncidentProgressStatus.ACTIVE) {
            throw new IllegalStateException("Инцидент звонка не активен");
        }

        DialogProgressStatus status = context.getDialogStatus();
        if ((status == DialogProgressStatus.IN_CALL
                || (status == DialogProgressStatus.DISCONNECTED && context.getTargetType() != IncidentTargetType.DDS))
                && context.getActiveCallId() != null
                && !context.getActiveCallId().equals(requestedCallId)) {
            throw new IllegalStateException("Другой звонок уже активен: " + context.getActiveCallId());
        }
        context.setActiveCallId(requestedCallId);
        context.setDialogStatus(DialogProgressStatus.IN_CALL);
        call.setStatus(CallStatus.ACTIVE);
        return toDialogProgress(contextStore.save(context));
    }

    @Transactional
    public DialogProgress completeCall(String id, String callId) {
        TrainingContext context = requireActiveCall(id, callId);
        CallSnapshot call = findCall(context, parseUuid(callId));
        call.setStatus(CallStatus.COMPLETED);
        context.setDialogStatus(DialogProgressStatus.COMPLETED);
        contextStore.save(context);
        if (context.getTargetType() == IncidentTargetType.SYSTEM_112) {
            completeSystem112IncidentIfNeeded(context, call);
        }
        return toDialogProgress(context);
    }

    @Transactional
    public DialogProgress disconnectCall(String id, String callId) {
        TrainingContext context = requireActiveCall(id, callId);
        if (context.getDialogStatus() == DialogProgressStatus.IN_CALL) {
            context.setDialogStatus(DialogProgressStatus.DISCONNECTED);
            findCall(context, parseUuid(callId)).setStatus(CallStatus.DISCONNECTED);
        }
        return toDialogProgress(contextStore.save(context));
    }

    @Transactional(readOnly = true)
    public CallSnapshot getCall(String id, String callId) {
        TrainingContext context = find(id);
        UUID requestedCallId = parseUuid(callId);
        if (requestedCallId.equals(context.getActiveCallId())
                && (context.getDialogStatus() == DialogProgressStatus.IN_CALL
                    || context.getDialogStatus() == DialogProgressStatus.DISCONNECTED)) {
            return findCall(context, requestedCallId);
        }
        return requireCall(context, requestedCallId);
    }

    @Transactional(readOnly = true)
    public CallSnapshot getNextCall(String id, String currentCallId) {
        TrainingContext context = find(id);
        List<CallSnapshot> sequence = context.getTargetType() == IncidentTargetType.DDS
                ? availableCalls(context) : flattenCalls(context);
        if (sequence.isEmpty()) {
            throw new IllegalStateException("У уровня контекста " + id + " нет звонков");
        }
        if ("-1".equals(currentCallId)) {
            return sequence.get(0);
        }
        UUID currentId = parseUuid(currentCallId);
        int currentIndex = -1;
        for (int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i).getSourceId().equals(currentId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) {
            throw new IllegalArgumentException("Звонок " + currentCallId + " не относится к контексту " + id);
        }
        return currentIndex + 1 >= sequence.size()
                ? null
                : sequence.get(currentIndex + 1);
    }

    private TrainingContext requireActiveCall(String id, String callId) {
        TrainingContext context = find(id);
        UUID requestedCallId = parseUuid(callId);
        if (context.getActiveCallId() == null || !context.getActiveCallId().equals(requestedCallId)) {
            throw new IllegalArgumentException("Звонок " + callId + " не является активным");
        }
        return context;
    }

    private DialogProgress toDialogProgress(TrainingContext context) {
        DialogProgressStatus status = context.getDialogStatus() == null
                ? DialogProgressStatus.IDLE : context.getDialogStatus();
        return new DialogProgress(context.getId(), context.getActiveCallId(), status);
    }

    private CallSnapshot requireCall(TrainingContext context, UUID callId) {
        return availableCalls(context).stream()
                .filter(call -> call.getSourceId().equals(callId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Звонок " + callId + " сейчас недоступен в контексте " + context.getId()));
    }

    private CallSnapshot findCall(TrainingContext context, UUID callId) {
        return context.getIncidents().stream()
                .flatMap(incident -> incident.getStages().stream())
                .flatMap(stage -> stage.getCalls().stream())
                .filter(call -> call.getSourceId().equals(callId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Звонок не найден: " + callId));
    }

    private List<CallSnapshot> availableCalls(TrainingContext context) {
        if (context.getTargetType() == IncidentTargetType.DDS) {
            return context.getIncidents().stream()
                    .filter(incident -> incident.getStatus() == IncidentProgressStatus.ACTIVE)
                    .flatMap(incident -> incident.getStages().stream()
                            .filter(stage -> stage.getSourceId().equals(incident.getActiveStageId())))
                    .flatMap(stage -> stage.getCalls().stream())
                    .toList();
        }
        return flattenCalls(context);
    }

    private List<CallSnapshot> flattenCalls(TrainingContext context) {
        return context.getIncidents().stream()
                .flatMap(incident -> incident.getStages().stream())
                .flatMap(stage -> stage.getCalls().stream())
                .filter(call -> call.getQueuePosition() != null)
                .sorted(Comparator.comparing(CallSnapshot::getQueuePosition))
                .toList();
    }

    private void completeSystem112IncidentIfNeeded(TrainingContext context, CallSnapshot completedCall) {
        var incident = incidentForCall(context, completedCall.getSourceId());
        boolean completed = incident.getStages().stream()
                .flatMap(stage -> stage.getCalls().stream())
                .allMatch(call -> call.getStatus() == CallStatus.COMPLETED);
        if (!completed) {
            return;
        }
        incident.setStatus(IncidentProgressStatus.COMPLETED);
        if (context.getExecutionMode() == ExecutionMode.SEQUENTIAL) {
            context.getIncidents().stream()
                    .filter(value -> value.getStatus() == IncidentProgressStatus.PENDING)
                    .min(Comparator.comparingInt(value -> value.getPosition()))
                    .ifPresent(value -> value.setStatus(IncidentProgressStatus.ACTIVE));
        }
        if (context.getIncidents().stream()
                .allMatch(value -> value.getStatus() == IncidentProgressStatus.COMPLETED)) {
            context.setStatus(ContextStatus.FILLED);
        }
        contextStore.save(context);
    }

    private com.simulator112.contextmanager.domain.common.IncidentSnapshot incidentForCall(
            TrainingContext context, UUID callId) {
        return context.getIncidents().stream()
                .filter(incident -> incident.getStages().stream().flatMap(stage -> stage.getCalls().stream())
                        .anyMatch(call -> call.getSourceId().equals(callId)))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Инцидент звонка не найден: " + callId));
    }

    private TrainingContext find(String id) {
        return contextStore.findById(parseUuid(id))
                .orElseThrow(() -> new IllegalArgumentException("Контекста не существует: " + id));
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Невалидный UUID: " + raw);
        }
    }
}
