package com.simulator112.review_service.adapter.out.course;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CourseServiceTeacherStudentAccessAdapterTests {
    @Test
    void returnsTrueWhenStudentBelongsToOwnedGroup() {
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new CourseServiceTeacherStudentAccessAdapter(builder, "http://course-service");

        server.expect(once(), requestTo("http://course-service/api/v1/study-groups"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", teacherId.toString()))
                .andRespond(withSuccess("""
                        [
                          { "id": "%s", "ownerId": "%s", "studentIds": ["%s"] }
                        ]
                        """.formatted(UUID.randomUUID(), teacherId, studentId), MediaType.APPLICATION_JSON));

        assertThat(adapter.isStudentOfTeacher(teacherId, studentId)).isTrue();
        server.verify();
    }

    @Test
    void returnsFalseWhenStudentNotInAnyOwnedGroup() {
        UUID teacherId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new CourseServiceTeacherStudentAccessAdapter(builder, "http://course-service");

        server.expect(once(), requestTo("http://course-service/api/v1/study-groups"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(adapter.isStudentOfTeacher(teacherId, UUID.randomUUID())).isFalse();
        server.verify();
    }

    @Test
    void returnsFalseWhenCourseServiceIsUnavailable() {
        UUID teacherId = UUID.randomUUID();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new CourseServiceTeacherStudentAccessAdapter(builder, "http://course-service");

        server.expect(once(), requestTo("http://course-service/api/v1/study-groups"))
                .andRespond(withServerError());

        assertThat(adapter.isStudentOfTeacher(teacherId, UUID.randomUUID())).isFalse();
        server.verify();
    }
}
