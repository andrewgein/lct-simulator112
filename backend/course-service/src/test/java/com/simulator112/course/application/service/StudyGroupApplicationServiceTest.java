package com.simulator112.course.application.service;

import com.simulator112.course.application.port.out.StudyGroupRepository;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.group.StudyGroup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyGroupApplicationServiceTest {
    private final StudyGroupRepository repository = mock(StudyGroupRepository.class);
    private final StudyGroupApplicationService service = new StudyGroupApplicationService(repository);

    @Test
    void deletesGroupByOwner() {
        UUID groupId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(repository.findById(groupId)).thenReturn(Optional.of(group(groupId, ownerId)));

        service.deleteStudyGroup(groupId, ownerId);

        verify(repository).deleteById(groupId);
    }

    @Test
    void rejectsDeleteByForeignOwner() {
        UUID groupId = UUID.randomUUID();
        when(repository.findById(groupId)).thenReturn(Optional.of(group(groupId, UUID.randomUUID())));

        assertThatThrownBy(() -> service.deleteStudyGroup(groupId, UUID.randomUUID()))
                .isInstanceOf(CourseAccessDeniedException.class);
    }

    private StudyGroup group(UUID groupId, UUID ownerId) {
        return new StudyGroup(groupId, "Группа", ownerId, List.of(UUID.randomUUID()));
    }
}
