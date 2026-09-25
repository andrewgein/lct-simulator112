package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.CourseView;
import com.simulator112.course.adapter.in.rest.dto.EnrollmentView;
import com.simulator112.course.adapter.in.rest.dto.StudyGroupRequest;
import com.simulator112.course.adapter.in.rest.dto.StudyGroupView;
import com.simulator112.course.application.port.in.AssignCourseToGroupUseCase;
import com.simulator112.course.application.port.in.CreateStudyGroupUseCase;
import com.simulator112.course.application.port.in.DeleteStudyGroupUseCase;
import com.simulator112.course.application.port.in.FindGroupCoursesUseCase;
import com.simulator112.course.application.port.in.FindOwnedStudyGroupsUseCase;
import com.simulator112.course.application.port.in.GetStudyGroupUseCase;
import com.simulator112.course.application.port.in.UpdateStudyGroupUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/study-groups")
@RequiredArgsConstructor
public class StudyGroupRestController {
    private final CreateStudyGroupUseCase createStudyGroup;
    private final UpdateStudyGroupUseCase updateStudyGroup;
    private final DeleteStudyGroupUseCase deleteStudyGroup;
    private final GetStudyGroupUseCase getStudyGroup;
    private final FindOwnedStudyGroupsUseCase findOwnedStudyGroups;
    private final AssignCourseToGroupUseCase assignCourseToGroup;
    private final FindGroupCoursesUseCase findGroupCourses;
    private final CourseRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudyGroupView create(@RequestHeader("X-User-Id") UUID userId,
                                 @Valid @RequestBody StudyGroupRequest request) {
        return mapper.toView(createStudyGroup.createStudyGroup(mapper.toDomain(null, request, userId)));
    }

    @PutMapping("/{groupId}")
    public StudyGroupView update(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID groupId,
                                 @Valid @RequestBody StudyGroupRequest request) {
        return mapper.toView(updateStudyGroup.updateStudyGroup(groupId, mapper.toDomain(groupId, request, userId),
                userId));
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID groupId) {
        deleteStudyGroup.deleteStudyGroup(groupId, userId);
    }

    @GetMapping("/{groupId}")
    public StudyGroupView get(@PathVariable UUID groupId) {
        return mapper.toView(getStudyGroup.getStudyGroup(groupId));
    }

    @GetMapping
    public List<StudyGroupView> findOwned(@RequestHeader("X-User-Id") UUID userId) {
        return findOwnedStudyGroups.findOwnedStudyGroups(userId).stream().map(mapper::toView).toList();
    }

    @GetMapping("/{groupId}/courses")
    public List<CourseView> findCourses(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID groupId) {
        return findGroupCourses.findGroupCourses(groupId, userId).stream().map(mapper::toView).toList();
    }

    @PostMapping("/{groupId}/courses/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentView assignCourse(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID groupId,
                                       @PathVariable UUID courseId) {
        return mapper.toView(assignCourseToGroup.assignCourseToGroup(courseId, groupId, userId));
    }
}
