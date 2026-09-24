package com.simulator112.contextmanager.application.port.out;

import com.simulator112.contextmanager.domain.common.TrainingContext;

public interface ReviewPort {
    boolean send(TrainingContext context);
}
