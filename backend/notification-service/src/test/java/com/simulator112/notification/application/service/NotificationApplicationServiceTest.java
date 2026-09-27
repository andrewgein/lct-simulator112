package com.simulator112.notification.application.service;

import com.simulator112.notification.application.model.CertificateNotice;
import com.simulator112.notification.application.model.ReviewCommentNotice;
import com.simulator112.notification.application.port.out.MailDelivery;
import com.simulator112.notification.application.port.out.NotificationStore;
import com.simulator112.notification.application.port.out.UserDirectory;
import com.simulator112.notification.domain.exception.NotificationNotFoundException;
import com.simulator112.notification.domain.model.EmailDeliveryStatus;
import com.simulator112.notification.domain.model.Notification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {
    @Mock private NotificationStore store;
    @Mock private UserDirectory users;
    @Mock private MailDelivery mail;
    @InjectMocks private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "appBaseUrl", "https://app.example.com/");
    }

    @Test
    void reviewCommentCreatesNotificationAndSendsMailOnce() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        ReviewCommentNotice notice = new ReviewCommentNotice(eventId, userId, reviewId, " Комментарий ", Instant.now());
        when(store.findByEventId(eventId)).thenReturn(Optional.empty());
        when(store.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));
        when(users.requireEmail(userId)).thenReturn("user@example.com");

        service.handleReviewComment(notice);

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(store, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getTargetUrl()).isEqualTo("/review/" + reviewId + "#comments");
        assertThat(saved.getAllValues().get(0).getText()).isEqualTo("Комментарий");
        assertThat(saved.getAllValues().get(1).getEmailStatus()).isEqualTo(EmailDeliveryStatus.SENT);
        verify(mail).send(eq(eventId), eq(userId), eq("user@example.com"), any(),
                org.mockito.ArgumentMatchers.contains("https://app.example.com/review/" + reviewId + "#comments"));
    }

    @Test
    void alreadySentReviewCommentDoesNotResendMail() {
        UUID eventId = UUID.randomUUID();
        Notification existing = new Notification();
        existing.setEmailStatus(EmailDeliveryStatus.SENT);
        when(store.findByEventId(eventId)).thenReturn(Optional.of(existing));

        service.handleReviewComment(new ReviewCommentNotice(eventId, UUID.randomUUID(), UUID.randomUUID(),
                "Комментарий", Instant.now()));

        verify(mail, never()).send(any(), any(), any(), any(), any());
        verify(store, never()).save(any());
    }

    @Test
    void certificateEventIsIdempotent() {
        UUID eventId = UUID.randomUUID();
        when(store.findByEventId(eventId)).thenReturn(Optional.empty());
        when(store.save(any(Notification.class))).thenAnswer(call -> call.getArgument(0));

        service.handleCertificateIssued(new CertificateNotice(eventId, UUID.randomUUID(), UUID.randomUUID(),
                "HONORS", "Курс", 95, Instant.now()));

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(store).save(saved.capture());
        assertThat(saved.getValue().getTitle()).contains("с отличием");
        assertThat(saved.getValue().getEmailStatus()).isEqualTo(EmailDeliveryStatus.SENT);
    }

    @Test
    void markReadRequiresNotificationOwnedByUser() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(store.findByIdAndUserId(notificationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(notificationId, userId))
                .isInstanceOf(NotificationNotFoundException.class);
        verify(store, never()).save(any());
    }
}
