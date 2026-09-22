package com.simulator112.course.adapter.out.persistence;

import com.simulator112.course.adapter.out.persistence.repository.SpringDataStudyGroupRepository;
import com.simulator112.course.application.port.out.StudyGroupRepository;
import com.simulator112.course.domain.group.StudyGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class StudyGroupPersistenceAdapter implements StudyGroupRepository {
    private final SpringDataStudyGroupRepository repository;
    private final CoursePersistenceMapper mapper;

    @Override
    @Transactional
    public StudyGroup save(StudyGroup studyGroup) {
        return mapper.toDomain(repository.save(mapper.toEntity(studyGroup)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudyGroup> findById(UUID groupId) {
        return repository.findById(groupId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyGroup> findAllByOwnerId(UUID ownerId) {
        return repository.findAllByOwnerId(ownerId).stream().map(mapper::toDomain).toList();
    }
}
