package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.CertificateNotFoundException;
import com.simulator112.review_service.application.port.in.GetCertificatesUseCase;
import com.simulator112.review_service.application.port.out.CertificateNotificationPort;
import com.simulator112.review_service.application.port.out.CertificateStore;
import com.simulator112.review_service.application.port.out.CourseAssignmentsPort;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.Certificate;
import com.simulator112.review_service.domain.model.CertificateType;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateApplicationService implements GetCertificatesUseCase {
    private static final int HONORS_THRESHOLD = 80;
    private static final int PASS_THRESHOLD = 50;

    private final CertificateStore store;
    private final CourseAssignmentsPort courseAssignments;
    private final ReviewStore reviews;
    private final CertificateNotificationPort notifications;

    @Transactional
    public void issueIfEligible(Review review) {
        var course = courseAssignments.findCourseForAssignment(review.assignmentId(), review.userId()).orElse(null);
        if (course == null || course.assignmentIds().isEmpty()) return;
        if (store.findByUserIdAndCourseId(review.userId(), course.courseId()).isPresent()) return;

        var latestByAssignment = reviews.findByUserId(review.userId()).stream()
                .filter(candidate -> candidate.status() == ReviewStatus.DONE)
                .collect(Collectors.toMap(Review::assignmentId, Function.identity(),
                        (first, second) -> first.createdAt().isAfter(second.createdAt()) ? first : second));
        Set<UUID> courseAssignmentIds = Set.copyOf(course.assignmentIds());
        if (!latestByAssignment.keySet().containsAll(courseAssignmentIds)) return;

        int score = 0;
        int maxScore = 0;
        for (UUID assignmentId : courseAssignmentIds) {
            var completed = latestByAssignment.get(assignmentId);
            score += completed.finalScore() == null ? completed.automaticScore() : completed.finalScore();
            maxScore += completed.maxScore();
        }
        int percent = maxScore == 0 ? 0 : (int) Math.round(score * 100.0 / maxScore);
        if (percent < PASS_THRESHOLD) return;

        var certificate = new Certificate(UUID.randomUUID(), review.userId(), course.courseId(),
                course.courseTitle(), percent,
                percent >= HONORS_THRESHOLD ? CertificateType.HONORS : CertificateType.COMPLETION, Instant.now());
        var saved = store.save(certificate);
        notifications.publish(saved);
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
