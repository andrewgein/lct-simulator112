package com.simulator112.contextmanager.application.port.in;

import com.simulator112.contextmanager.application.model.DialogCall;
import com.simulator112.contextmanager.domain.common.DialogProgress;
import com.simulator112.contextmanager.domain.common.DialogTranscript;

public interface CallUseCase {
    void appendDialog(String contextId, String callId, DialogTranscript dialog);
    DialogProgress getDialogProgress(String contextId);
    DialogProgress startCall(String contextId, String callId);
    DialogProgress completeCall(String contextId, String callId);
    DialogProgress disconnectCall(String contextId, String callId);
    DialogCall getCall(String contextId, String callId);
    DialogCall getNextCall(String contextId, String currentCallId);
    DialogTranscript getCallTranscript(String contextId, String callId);
    void clearCallTranscript(String contextId, String callId);
}
