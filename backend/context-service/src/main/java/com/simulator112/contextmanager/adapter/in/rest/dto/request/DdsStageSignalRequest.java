package com.simulator112.contextmanager.adapter.in.rest.dto.request;

import com.simulator112.contextmanager.domain.dds.DdsStageSignal;
import jakarta.validation.constraints.NotNull;

public record DdsStageSignalRequest(@NotNull DdsStageSignal signal) {
}
