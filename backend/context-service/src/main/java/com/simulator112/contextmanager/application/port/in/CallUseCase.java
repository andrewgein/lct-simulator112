package com.simulator112.contextmanager.application.port.in;

import com.simulator112.contextmanager.domain.common.CallSnapshot;
import com.simulator112.contextmanager.domain.common.DialogProgress;
import com.simulator112.contextmanager.domain.common.DialogTranscript;

public interface CallUseCase {
    void appendDialog(String contextId, DialogTranscript dialog);
    DialogProgress getDialogProgress(String contextId);
    DialogProgress startCall(String contextId, String callId);
    DialogProgress completeCall(String contextId, String callId);
    DialogProgress disconnectCall(String contextId, String callId);
    CallSnapshot getCall(String contextId, String callId);
    CallSnapshot getNextCall(String contextId, String currentCallId);
}
