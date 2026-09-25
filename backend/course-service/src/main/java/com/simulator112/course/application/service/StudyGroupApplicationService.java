package com.simulator112.course.application.service;

import com.simulator112.course.application.port.in.CreateStudyGroupUseCase;
import com.simulator112.course.application.port.in.DeleteStudyGroupUseCase;
import com.simulator112.course.application.port.in.FindOwnedStudyGroupsUseCase;
import com.simulator112.course.application.port.in.GetStudyGroupUseCase;
import com.simulator112.course.application.port.in.UpdateStudyGroupUseCase;
import com.simulator112.course.application.port.out.StudyGroupRepository;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.StudyGroupNotFoundException;
import com.simulator112.course.domain.group.StudyGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudyGroupApplicationService implements CreateStudyGroupUseCase, UpdateStudyGroupUseCase,
        DeleteStudyGroupUseCase, GetStudyGroupUseCase, FindOwnedStudyGroupsUseCase {

    private final StudyGroupRepository studyGroupRepository;

    @Override
    @Transactional
    public StudyGroup createStudyGroup(StudyGroup studyGroup) {
        validate(studyGroup);
        return studyGroupRepository.save(studyGroup);
    }

    @Override
    @Transactional
    public StudyGroup updateStudyGroup(UUID groupId, StudyGroup studyGroup, UUID requesterId) {
        StudyGroup existing = getStudyGroup(groupId);
        if (!existing.ownerId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Изменять группу может только её владелец");
        }
        StudyGroup updated = new StudyGroup(groupId, studyGroup.title(), existing.ownerId(), studyGroup.studentIds());
        validate(updated);
        return studyGroupRepository.save(updated);
    }

    @Override
    @Transactional
    public void deleteStudyGroup(UUID groupId, UUID requesterId) {
        StudyGroup existing = getStudyGroup(groupId);
        if (!existing.ownerId().equals(requesterId)) {
            throw new CourseAccessDeniedException("Удалить группу может только её владелец");
        }
        studyGroupRepository.deleteById(groupId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyGroup getStudyGroup(UUID groupId) {
        return studyGroupRepository.findById(groupId).orElseThrow(() -> new StudyGroupNotFoundException(groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyGroup> findOwnedStudyGroups(UUID ownerId) {
        return studyGroupRepository.findAllByOwnerId(ownerId);
    }

    private void validate(StudyGroup studyGroup) {
        if (studyGroup == null) {
            throw new IllegalArgumentException("Учебная группа обязательна");
        }
        if (studyGroup.ownerId() == null) {
            throw new IllegalArgumentException("Владелец группы обязателен");
        }
        if (studyGroup.title() == null || studyGroup.title().isBlank()) {
            throw new IllegalArgumentException("Название группы обязательно");
        }
        if (new HashSet<>(studyGroup.studentIds()).size() != studyGroup.studentIds().size()) {
            throw new IllegalArgumentException("Слушатель не может повторяться внутри группы");
        }
    }
}
