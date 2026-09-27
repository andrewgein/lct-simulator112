package com.simulator112.course.application;

import com.simulator112.course.application.port.out.AssignmentResultsPort;
import com.simulator112.course.application.port.out.CertificateStore;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.application.service.CertificateApplicationService;
import com.simulator112.course.domain.course.Assignment;
import com.simulator112.course.domain.course.AssignmentDifficulty;
import com.simulator112.course.domain.course.AssignmentExecutionMode;
import com.simulator112.course.domain.course.Course;
import com.simulator112.course.domain.course.CourseTargetType;
import com.simulator112.course.domain.enrollment.Enrollment;
import com.simulator112.course.domain.model.Certificate;
import com.simulator112.course.domain.model.CertificateType;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CertificateApplicationServiceTests {
    private final CertificateStore store = mock(CertificateStore.class);
    private final CourseRepository courses = mock(CourseRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final AssignmentResultsPort results = mock(AssignmentResultsPort.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CertificateApplicationService service = new CertificateApplicationService(
            store, courses, enrollments, results, events);
    private final UUID user = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    private void setup(UUID... assignmentIds) {
        var ids = assignmentIds.length == 0 ? new UUID[]{first, second} : assignmentIds;
        when(enrollments.findAllByStudentId(user)).thenReturn(List.of(new Enrollment(UUID.randomUUID(), courseId, UUID.randomUUID())));
        when(courses.findById(courseId)).thenReturn(Optional.of(new Course(courseId, "Курс", null,
                CourseTargetType.SYSTEM_112, UUID.randomUUID(), List.of(), java.util.Arrays.stream(ids)
                .map(id -> new Assignment(id, "Задание", null, AssignmentDifficulty.NORMAL,
                        AssignmentExecutionMode.SEQUENTIAL, List.of())).toList())));
        when(store.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void missingResultDoesNotIssueCertificate() {
        setup();
        when(results.get(user, List.of(first, second))).thenReturn(List.of(new AssignmentResultsPort.Result(first, 80, 100, 4)));
        service.checkCompletion(user, second);
        verify(store, never()).save(any());
    }

    @Test
    void issuesCertificateWhenAllAssignmentsPassedAtConfiguredThresholds() {
        setup();
        when(results.get(user, List.of(first, second))).thenReturn(List.of(
                new AssignmentResultsPort.Result(first, 24, 100, 3),
                new AssignmentResultsPort.Result(second, 24, 100, 3)));

        service.checkCompletion(user, second);

        var certificate = org.mockito.ArgumentCaptor.forClass(Certificate.class);
        verify(store).save(certificate.capture());
        assertThat(certificate.getValue().percent()).isEqualTo(24);
        assertThat(certificate.getValue().type()).isEqualTo(CertificateType.COMPLETION);
    }

    @Test
    void doesNotIssueBelowAverageThreeOrWithUngradedAssignment() {
        setup();
        when(results.get(user, List.of(first, second))).thenReturn(List.of(
                new AssignmentResultsPort.Result(first, 90, 100, 3),
                new AssignmentResultsPort.Result(second, 30, 100, 2)));
        service.checkCompletion(user, second);
        when(results.get(user, List.of(first, second))).thenReturn(List.of(
                new AssignmentResultsPort.Result(first, 90, 100, 5),
                new AssignmentResultsPort.Result(second, 30, 100, null)));
        service.checkCompletion(user, second);
        verify(store, never()).save(any());
    }

    @Test
    void averageThreePassesEvenIfOneAssignmentWasFailed() {
        setup();
        when(results.get(user, List.of(first, second))).thenReturn(List.of(
                new AssignmentResultsPort.Result(first, 80, 100, 4),
                new AssignmentResultsPort.Result(second, 20, 100, 2)));
        service.checkCompletion(user, second);
        var certificate = org.mockito.ArgumentCaptor.forClass(Certificate.class);
        verify(store).save(certificate.capture());
        assertThat(certificate.getValue().type()).isEqualTo(CertificateType.COMPLETION);
    }

    @Test
    void issuesOnceWithAggregateScoreAndHonors() {
        setup();
        when(results.get(user, List.of(first, second))).thenReturn(List.of(
                new AssignmentResultsPort.Result(first, 90, 100, 5),
                new AssignmentResultsPort.Result(second, 70, 100, 5)));
        service.checkCompletion(user, second);
        var certificate = org.mockito.ArgumentCaptor.forClass(Certificate.class);
        verify(store).save(certificate.capture());
        assertThat(certificate.getValue().percent()).isEqualTo(80);
        assertThat(certificate.getValue().type()).isEqualTo(CertificateType.HONORS);
        verify(events).publishEvent(any(Certificate.class));

        when(store.findByUserIdAndCourseId(user, courseId)).thenReturn(Optional.of(certificate.getValue()));
        service.checkCompletion(user, second);
        verify(store, times(1)).save(any());
        verify(results, times(1)).get(user, List.of(first, second));
    }

    @Test
    void averageExactlyFourPointSevenFiveIsHonors() {
        var third = UUID.randomUUID();
        var fourth = UUID.randomUUID();
        var ids = List.of(first, second, third, fourth);
        setup(first, second, third, fourth);
        when(results.get(user, ids)).thenReturn(List.of(
                new AssignmentResultsPort.Result(first, 80, 100, 5),
                new AssignmentResultsPort.Result(second, 80, 100, 5),
                new AssignmentResultsPort.Result(third, 80, 100, 5),
                new AssignmentResultsPort.Result(fourth, 60, 100, 4)));

        service.checkCompletion(user, fourth);

        var certificate = org.mockito.ArgumentCaptor.forClass(Certificate.class);
        verify(store).save(certificate.capture());
        assertThat(certificate.getValue().type()).isEqualTo(CertificateType.HONORS);
    }
}
