package com.simulator112.contextmanager.domain.dds;

public enum DdsStageType {
    ASSIGN_BRIGADE,
    WAIT_FOR_BRIGADE_STATUS_CHANGE,
    CALL_BRIGADE_FOR_STATUS,
    REQUEST_ADDITIONAL_SERVICE,
    COMPLETE_INCIDENT
}
