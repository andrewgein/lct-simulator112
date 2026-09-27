package com.simulator112.course.application.service;

import com.simulator112.course.application.exception.CertificateNotFoundException;
import com.simulator112.course.application.port.in.GetCertificatesUseCase;
import com.simulator112.course.application.port.out.AssignmentResultsPort;
import com.simulator112.course.application.port.out.CertificateStore;
import com.simulator112.course.application.port.out.CourseRepository;
import com.simulator112.course.application.port.out.EnrollmentRepository;
import com.simulator112.course.domain.model.Certificate;
import com.simulator112.course.domain.model.CertificateType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateApplicationService implements GetCertificatesUseCase {
    private static final int PASSING_AVERAGE = 3;
    private static final int HONORS_AVERAGE_QUARTERS = 19;

    private final CertificateStore store;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final AssignmentResultsPort results;
    private final ApplicationEventPublisher events;

    @Transactional
    public void checkCompletion(UUID userId, UUID assignmentId) {
        var courseIds = enrollments.findAllByStudentId(userId).stream()
                .map(enrollment -> enrollment.courseId()).distinct().toList();
        for (UUID courseId : courseIds) {
            var course = courses.findById(courseId).orElse(null);
            if (course == null || course.deletedAt() != null || course.assignment(assignmentId).isEmpty()
                    || course.assignments().isEmpty() || store.findByUserIdAndCourseId(userId, courseId).isPresent()) continue;

            var ids = course.assignments().stream().map(assignment -> assignment.id()).toList();
            var scores = results.get(userId, ids).stream().collect(Collectors.toMap(
                    AssignmentResultsPort.Result::assignmentId, result -> result, (first, second) -> first));
            if (!scores.keySet().containsAll(Set.copyOf(ids)) || ids.stream()
                    .anyMatch(id -> scores.get(id).grade() == null)) continue;
            int total = 0;
            int max = 0;
            int gradeSum = 0;
            for (UUID id : ids) {
                var result = scores.get(id);
                total += result.score();
                max += result.maxScore();
                gradeSum += result.grade();
            }
            if (gradeSum < PASSING_AVERAGE * ids.size()) continue;
            int percent = max <= 0 ? 0 : (int) Math.round(total * 100.0 / max);
            var saved = store.save(new Certificate(UUID.randomUUID(), userId, courseId, course.title(), percent,
                    gradeSum * 4 >= HONORS_AVERAGE_QUARTERS * ids.size()
                            ? CertificateType.HONORS : CertificateType.COMPLETION, Instant.now()));
            events.publishEvent(saved);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Certificate> getByUserId(UUID userId) {
        return store.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Certificate getById(UUID id, UUID userId) {
        var certificate = store.findById(id).orElseThrow(() -> new CertificateNotFoundException(id));
        if (!certificate.userId().equals(userId)) throw new CertificateNotFoundException(id);
        return certificate;
    }
}
