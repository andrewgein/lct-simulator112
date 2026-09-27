package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.adapter.in.rest.dto.AssignmentRequest;
import com.simulator112.course.adapter.in.rest.dto.CourseRequest;
import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
import com.simulator112.course.domain.course.CourseTargetType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRestMapperGradeTest {
    @Test
    void roundTripsGradeThresholdsInCourseRequestAndView() {
        var request = new CourseRequest("Курс", null, CourseTargetType.SYSTEM_112, null, List.of(),
                List.of(new AssignmentRequest(null, "Задание", null, AssignmentDifficulty.NORMAL,
                        AssignmentExecutionMode.SEQUENTIAL, List.of(UUID.randomUUID()), 40, 60, 80)));
        var mapper = new CourseRestMapper();

        var course = mapper.toDomain(null, request, UUID.randomUUID());
        var view = mapper.toView(course).assignments().getFirst();

        assertThat(view.threshold3()).isEqualTo(40);
        assertThat(view.threshold4()).isEqualTo(60);
        assertThat(view.threshold5()).isEqualTo(80);
    }
}
