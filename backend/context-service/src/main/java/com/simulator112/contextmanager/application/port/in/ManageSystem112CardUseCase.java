package com.simulator112.contextmanager.application.port.in;

import com.simulator112.contextmanager.application.model.system112.SolutionContextRequest;
import com.simulator112.contextmanager.application.model.system112.SolutionContextSaveResponse;
import com.simulator112.contextmanager.application.model.system112.SolutionContextView;
import java.util.List;
import java.util.UUID;

public interface ManageSystem112CardUseCase {
    SolutionContextSaveResponse createCardForCall(UUID contextId, UUID callId, SolutionContextRequest request);
    SolutionContextSaveResponse saveCardRevision(UUID contextId, UUID cardId, SolutionContextRequest request);
    List<SolutionContextView> getCards(UUID contextId);
}
