package com.simulator112.contextmanager.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.simulator112.context.grpc.contract.DialogContext;
import com.simulator112.context.grpc.contract.DialogProgress;
import com.simulator112.contextmanager.grpc.mapper.DialogContextMapper;
import com.simulator112.contextmanager.grpc.mapper.IncidentContextMapper;
import com.simulator112.contextmanager.model.entity.Context;
import com.simulator112.contextmanager.model.entity.DialupContextEntity;
import com.simulator112.contextmanager.model.enums.DialogProgressStatus;
import com.simulator112.contextmanager.repository.ContextRepository;
import com.simulator112.incident.grpc.contract.DialupContext;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DialogService {
    private final ContextRepository contextRepository;

    @Transactional
    public void appendDialogContext(String id, DialogContext dialogContext) {
        Context context = find(id);
        if (context.getDialogContext() == null) {
            context.attachDialogContext(DialogContextMapper.toEntity(dialogContext));
        } else {
            DialogContextMapper.appendToEntity(context.getDialogContext(), dialogContext);
        }
        contextRepository.save(context);
    }

    @Transactional(readOnly = true)
    public DialogProgress getDialogProgress(String id) {
        return toDialogProgress(find(id));
    }

    @Transactional
    public DialogProgress startDialup(String id, String dialupId) {
        Context context = find(id);
        UUID requestedDialupId = parseUuid(dialupId);
        requireDialup(context, requestedDialupId);

        DialogProgressStatus status = context.getDialogStatus();
        if ((status == DialogProgressStatus.IN_CALL || status == DialogProgressStatus.DISCONNECTED)
                && context.getActiveDialupId() != null
                && !context.getActiveDialupId().equals(requestedDialupId)) {
            throw new IllegalStateException("Другой диалап уже активен: " + context.getActiveDialupId());
        }
        context.setActiveDialupId(requestedDialupId);
        context.setDialogStatus(DialogProgressStatus.IN_CALL);
        return toDialogProgress(contextRepository.save(context));
    }

    @Transactional
    public DialogProgress completeDialup(String id, String dialupId) {
        Context context = requireActiveDialup(id, dialupId);
        context.setDialogStatus(DialogProgressStatus.COMPLETED);
        return toDialogProgress(contextRepository.save(context));
    }

    @Transactional
    public DialogProgress disconnectDialup(String id, String dialupId) {
        Context context = requireActiveDialup(id, dialupId);
        if (context.getDialogStatus() == DialogProgressStatus.IN_CALL) {
            context.setDialogStatus(DialogProgressStatus.DISCONNECTED);
        }
        return toDialogProgress(contextRepository.save(context));
    }

    @Transactional(readOnly = true)
    public DialupContext getDialup(String id, String dialupId) {
        Context context = find(id);
        UUID requestedDialupId = parseUuid(dialupId);
        return flattenDialups(context).stream()
                .filter(dialup -> dialup.getSourceDialupId().equals(requestedDialupId))
                .findFirst()
                .map(IncidentContextMapper::toProto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Диалап " + dialupId + " не относится к контексту " + id));
    }

    @Transactional(readOnly = true)
    public DialupContext getNextDialup(String id, String currentDialupId) {
        Context context = find(id);
        List<DialupContextEntity> sequence = flattenDialups(context);
        if (sequence.isEmpty()) {
            throw new IllegalStateException("У уровня контекста " + id + " нет диалапов");
        }
        if ("-1".equals(currentDialupId)) {
            return IncidentContextMapper.toProto(sequence.get(0));
        }
        UUID currentId = parseUuid(currentDialupId);
        int currentIndex = -1;
        for (int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i).getSourceDialupId().equals(currentId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex == -1) {
            throw new IllegalArgumentException("Диалап " + currentDialupId + " не относится к контексту " + id);
        }
        return currentIndex + 1 >= sequence.size()
                ? null
                : IncidentContextMapper.toProto(sequence.get(currentIndex + 1));
    }

    private Context requireActiveDialup(String id, String dialupId) {
        Context context = find(id);
        UUID requestedDialupId = parseUuid(dialupId);
        if (context.getActiveDialupId() == null || !context.getActiveDialupId().equals(requestedDialupId)) {
            throw new IllegalArgumentException("Диалап " + dialupId + " не является активным");
        }
        return context;
    }

    private DialogProgress toDialogProgress(Context context) {
        DialogProgressStatus status = context.getDialogStatus() == null
                ? DialogProgressStatus.IDLE : context.getDialogStatus();
        return DialogProgress.newBuilder()
                .setContextId(context.getUuid().toString())
                .setActiveDialupId(context.getActiveDialupId() == null ? "" : context.getActiveDialupId().toString())
                .setStatus(com.simulator112.context.grpc.contract.DialogProgressStatus.valueOf(status.name()))
                .build();
    }

    private void requireDialup(Context context, UUID dialupId) {
        if (flattenDialups(context).stream()
                .noneMatch(dialup -> dialup.getSourceDialupId().equals(dialupId))) {
            throw new IllegalArgumentException("Диалап " + dialupId + " не относится к контексту " + context.getUuid());
        }
    }

    private List<DialupContextEntity> flattenDialups(Context context) {
        return context.getIncidentContexts().stream()
                .flatMap(incident -> incident.getStages().stream())
                .flatMap(stage -> stage.getDialups().stream())
                .sorted(Comparator.comparing(DialupContextEntity::getQueuePosition))
                .toList();
    }

    private Context find(String id) {
        return contextRepository.findById(parseUuid(id))
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
