package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.ReviewCommentForbiddenException;
import com.simulator112.review_service.application.exception.ReviewNotFoundException;
import com.simulator112.review_service.application.port.out.ReviewCommentStore;
import com.simulator112.review_service.application.port.out.ReviewCommentNotificationPort;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.application.port.out.TeacherStudentAccessPort;
import com.simulator112.review_service.domain.model.Review;
import com.simulator112.review_service.domain.model.ReviewComment;
import com.simulator112.review_service.domain.model.ReviewStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewCommentApplicationServiceTests {
    private final ReviewStore reviewStore = mock(ReviewStore.class);
    private final ReviewCommentStore commentStore = mock(ReviewCommentStore.class);
    private final ReviewCommentNotificationPort notificationPort = mock(ReviewCommentNotificationPort.class);
    private final TeacherStudentAccessPort teacherStudentAccess = mock(TeacherStudentAccessPort.class);
    private final ReviewCommentApplicationService service = new ReviewCommentApplicationService(
            reviewStore, commentStore, notificationPort, teacherStudentAccess);

    @Test
    void addsTrimmedCommentWhenSupervisorOwnsStudent() {
        UUID contextId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.of(review(contextId, studentId)));
        when(teacherStudentAccess.isStudentOfTeacher(authorId, studentId)).thenReturn(true);
        when(commentStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewComment result = service.add(contextId, authorId, "SUPERVISOR", "  Обратите внимание на адрес  ");

        assertThat(result.reviewContextId()).isEqualTo(contextId);
        assertThat(result.authorId()).isEqualTo(authorId);
        assertThat(result.text()).isEqualTo("Обратите внимание на адрес");
        verify(commentStore).save(any());
        verify(notificationPort).publish(result, studentId);
    }

    @Test
    void adminCanCommentWithoutOwnershipCheck() {
        UUID contextId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.of(review(contextId, studentId)));
        when(commentStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.add(contextId, authorId, "ADMIN", "Комментарий");

        verify(teacherStudentAccess, never()).isStudentOfTeacher(any(), any());
        verify(commentStore).save(any());
    }

    @Test
    void rejectsSupervisorCommentForForeignStudent() {
        UUID contextId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.of(review(contextId, studentId)));
        when(teacherStudentAccess.isStudentOfTeacher(authorId, studentId)).thenReturn(false);

        assertThatThrownBy(() -> service.add(contextId, authorId, "SUPERVISOR", "Комментарий"))
                .isInstanceOf(ReviewCommentForbiddenException.class);
        verify(commentStore, never()).save(any());
    }

    @Test
    void rejectsCommentForUnknownReview() {
        UUID contextId = UUID.randomUUID();
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(contextId, UUID.randomUUID(), "ADMIN", "Комментарий"))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void returnsCommentsInStoreOrder() {
        UUID contextId = UUID.randomUUID();
        ReviewComment first = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(),
                "Первый", Instant.parse("2026-01-01T10:00:00Z"));
        ReviewComment second = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(),
                "Второй", Instant.parse("2026-01-01T11:00:00Z"));
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.of(review(contextId, UUID.randomUUID())));
        when(commentStore.findByContextId(contextId)).thenReturn(List.of(first, second));

        assertThat(service.getByContextId(contextId)).containsExactly(first, second);
    }

    private Review review(UUID contextId, UUID userId) {
        return new Review(contextId, userId, UUID.randomUUID(), ReviewStatus.DONE, List.of(),
                0, 0, 0, 0, 30, 0, null, null, null, Instant.now(), Instant.now());
    }
}
