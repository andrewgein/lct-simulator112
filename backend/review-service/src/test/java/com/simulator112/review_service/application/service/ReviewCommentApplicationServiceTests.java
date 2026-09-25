package com.simulator112.review_service.application.service;

import com.simulator112.review_service.application.exception.ReviewNotFoundException;
import com.simulator112.review_service.application.port.out.ReviewCommentStore;
import com.simulator112.review_service.application.port.out.ReviewStore;
import com.simulator112.review_service.domain.model.ReviewComment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewCommentApplicationServiceTests {
    private final ReviewStore reviewStore = mock(ReviewStore.class);
    private final ReviewCommentStore commentStore = mock(ReviewCommentStore.class);
    private final ReviewCommentApplicationService service = new ReviewCommentApplicationService(reviewStore, commentStore);

    @Test
    void addsTrimmedCommentWithoutChangingReview() {
        UUID contextId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.of(mock()));
        when(commentStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewComment result = service.add(contextId, authorId, "  Обратите внимание на адрес  ");

        assertThat(result.reviewContextId()).isEqualTo(contextId);
        assertThat(result.authorId()).isEqualTo(authorId);
        assertThat(result.text()).isEqualTo("Обратите внимание на адрес");
        verify(commentStore).save(any());
    }

    @Test
    void rejectsCommentForUnknownReview() {
        UUID contextId = UUID.randomUUID();
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(contextId, UUID.randomUUID(), "Комментарий"))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    @Test
    void returnsCommentsInStoreOrder() {
        UUID contextId = UUID.randomUUID();
        ReviewComment first = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(),
                "Первый", Instant.parse("2026-01-01T10:00:00Z"));
        ReviewComment second = new ReviewComment(UUID.randomUUID(), contextId, UUID.randomUUID(),
                "Второй", Instant.parse("2026-01-01T11:00:00Z"));
        when(reviewStore.findByContextId(contextId)).thenReturn(Optional.of(mock()));
        when(commentStore.findByContextId(contextId)).thenReturn(List.of(first, second));

        assertThat(service.getByContextId(contextId)).containsExactly(first, second);
    }
}
