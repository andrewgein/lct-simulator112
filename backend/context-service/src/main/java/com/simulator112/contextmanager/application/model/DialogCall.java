package com.simulator112.contextmanager.application.model;

import com.simulator112.contextmanager.domain.common.Address;
import com.simulator112.contextmanager.domain.common.CallSnapshot;

public record DialogCall(CallSnapshot call, Address incidentAddress) {
}
