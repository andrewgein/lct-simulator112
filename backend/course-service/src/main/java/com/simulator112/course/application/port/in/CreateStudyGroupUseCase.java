package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.group.StudyGroup;

public interface CreateStudyGroupUseCase {
    StudyGroup createStudyGroup(StudyGroup studyGroup);
}
