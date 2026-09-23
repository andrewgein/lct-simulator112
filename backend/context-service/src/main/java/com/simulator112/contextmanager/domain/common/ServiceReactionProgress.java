package com.simulator112.contextmanager.domain.common;

import java.util.List;

public record ServiceReactionProgress(String serviceCode, ReactionStatus currentStatus,
                                      List<ReactionStatusEvent> history) {
    public ServiceReactionProgress {
        history = List.copyOf(history);
    }

    public static ServiceReactionProgress from(ServiceReaction reaction) {
        return new ServiceReactionProgress(
                reaction.getServiceCode(), reaction.currentStatus(), reaction.getHistory());
    }
}
