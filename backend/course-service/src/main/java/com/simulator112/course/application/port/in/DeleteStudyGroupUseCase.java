package com.simulator112.course.application.port.in;

import java.util.UUID;

public interface DeleteStudyGroupUseCase {
    void deleteStudyGroup(UUID groupId, UUID requesterId, String requesterRole);
}
