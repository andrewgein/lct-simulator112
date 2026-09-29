package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.group.StudyGroup;

import java.util.List;

public interface FindAllStudyGroupsUseCase {
    List<StudyGroup> findAllStudyGroups();
}
