package com.simulator112.course.application.port.out;

import com.simulator112.course.domain.group.StudyGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudyGroupRepository {
    StudyGroup save(StudyGroup studyGroup);

    Optional<StudyGroup> findById(UUID groupId);

    List<StudyGroup> findAll();

    List<StudyGroup> findAllByOwnerId(UUID ownerId);

    void deleteById(UUID groupId);
}
