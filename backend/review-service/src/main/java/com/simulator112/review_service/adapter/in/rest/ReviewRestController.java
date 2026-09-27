package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.ConfirmReviewRequest;
import com.simulator112.review_service.adapter.in.rest.dto.CallRecordingResponse;
import com.simulator112.review_service.adapter.in.rest.dto.CallRecordingsResponse;
import com.simulator112.review_service.adapter.in.rest.dto.PersonalStatisticsResponse;
import com.simulator112.review_service.adapter.in.rest.dto.ReviewResponse;
import com.simulator112.review_service.adapter.in.rest.dto.UpdateCriterionScoreRequest;
import com.simulator112.review_service.adapter.in.rest.dto.UpdateReviewCriteriaRequest;
import com.simulator112.review_service.adapter.in.rest.dto.UserReviewsResponse;
import com.simulator112.review_service.application.port.in.ConfirmReviewUseCase;
import com.simulator112.review_service.application.port.in.GetPersonalStatisticsUseCase;
import com.simulator112.review_service.application.port.in.GetReviewUseCase;
import com.simulator112.review_service.application.port.in.UpdateCriterionScoresUseCase;
import com.simulator112.review_service.application.port.out.CallRecordingStore;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewRestController {
    private static final Set<String> REVIEWER_ROLES = Set.of("ADMIN", "SUPERVISOR");
    private final GetReviewUseCase getReview;
    private final ConfirmReviewUseCase confirmReview;
    private final UpdateCriterionScoresUseCase updateCriterionScores;
    private final CallRecordingStore callRecordings;
    private final GetPersonalStatisticsUseCase getStatistics;

    @GetMapping
    public UserReviewsResponse getUserReviews(@RequestHeader("X-User-Id") UUID userId) {
        return reviewsFor(userId);
    }

    @GetMapping("/users/{studentId}")
    public UserReviewsResponse getStudentReviews(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID studentId) {
        requireReviewer(role);
        return reviewsFor(studentId);
    }

    @GetMapping("/statistics")
    public PersonalStatisticsResponse getMyStatistics(@RequestHeader("X-User-Id") UUID userId) {
        return ReviewRestMapper.toResponse(getStatistics.getForUser(userId));
    }

    @GetMapping("/users/{studentId}/statistics")
    public PersonalStatisticsResponse getStudentStatistics(
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID studentId) {
        requireReviewer(role);
        return ReviewRestMapper.toResponse(getStatistics.getForUser(studentId));
    }

    @GetMapping("/{contextId}")
    public ReviewResponse getReview(@RequestHeader("X-User-Id") UUID userId,
                                    @RequestHeader("X-User-Role") String role,
                                    @PathVariable UUID contextId) {
        var review = getReview.getByContextId(contextId);
        if (!userId.equals(review.userId()) && !REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Результат недоступен пользователю");
        }
        return ReviewRestMapper.toResponse(review);
    }

    @GetMapping("/{contextId}/recordings")
    public CallRecordingsResponse getRecordings(@RequestHeader("X-User-Id") UUID userId,
                                                 @RequestHeader("X-User-Role") String role,
                                                 @PathVariable UUID contextId) {
        requireReviewAccess(userId, role, contextId);
        return new CallRecordingsResponse(callRecordings.findByContextId(contextId).stream()
                .map(recording -> new CallRecordingResponse(recording.callId(), recording.fileName(),
                        recording.startedAt()))
                .toList());
    }

    @GetMapping("/{contextId}/recordings/{callId}/{fileName}")
    public ResponseEntity<byte[]> getRecording(@RequestHeader("X-User-Id") UUID userId,
                                                @RequestHeader("X-User-Role") String role,
                                                @PathVariable UUID contextId,
                                                @PathVariable String callId,
                                                @PathVariable String fileName) {
        requireReviewAccess(userId, role, contextId);
        byte[] content = callRecordings.findContent(contextId, callId, fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("audio/wav"))
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping("/{contextId}/confirm")
    public ReviewResponse confirmReview(@RequestHeader("X-User-Id") UUID expertId,
                                        @RequestHeader("X-User-Role") String role,
                                        @PathVariable UUID contextId,
                                        @Valid @RequestBody ConfirmReviewRequest request) {
        if (!"SUPERVISOR".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Подтверждать оценку может только преподаватель");
        }
        return ReviewRestMapper.toResponse(confirmReview.confirm(
                contextId, expertId, request.finalScore(), request.comment()));
    }

    @PatchMapping("/{contextId}/criteria")
    public ReviewResponse updateCriteria(@RequestHeader("X-User-Id") UUID expertId,
                                         @RequestHeader("X-User-Role") String role,
                                         @PathVariable UUID contextId,
                                         @Valid @RequestBody UpdateReviewCriteriaRequest request) {
        if (!REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Редактировать баллы может только преподаватель");
        }
        Map<UUID, Integer> corrections = request.corrections().stream()
                .collect(Collectors.toMap(UpdateCriterionScoreRequest::criterionResultId,
                        UpdateCriterionScoreRequest::score));
        return ReviewRestMapper.toResponse(updateCriterionScores.updateScores(contextId, expertId, corrections));
    }

    private UserReviewsResponse reviewsFor(UUID userId) {
        return new UserReviewsResponse(getReview.getByUserId(userId).stream()
                .map(ReviewRestMapper::toResponse).toList());
    }

    private void requireReviewer(String role) {
        if (!REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Просмотр результатов доступен преподавателям");
        }
    }

    private void requireReviewAccess(UUID userId, String role, UUID contextId) {
        var review = getReview.getByContextId(contextId);
        if (!userId.equals(review.userId()) && !REVIEWER_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Результат недоступен пользователю");
        }
    }
}
