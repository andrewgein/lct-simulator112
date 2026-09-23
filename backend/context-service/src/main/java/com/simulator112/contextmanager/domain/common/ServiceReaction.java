package com.simulator112.contextmanager.domain.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ServiceReaction {
    @Setter
    private UUID persistenceId;
    private final String serviceCode;
    private final List<ReactionStatusEvent> history;

    public ServiceReaction(String serviceCode) {
        this(serviceCode, new ArrayList<>());
    }

    public ServiceReaction(String serviceCode, List<ReactionStatusEvent> history) {
        this(null, serviceCode, history);
    }

    public ServiceReaction(UUID persistenceId, String serviceCode, List<ReactionStatusEvent> history) {
        this.persistenceId = persistenceId;
        this.serviceCode = serviceCode;
        this.history = new ArrayList<>(history);
    }

    public ReactionStatus currentStatus() {
        return history.isEmpty() ? null : history.getLast().status();
    }
}
