package com.keystone.service;

import com.keystone.config.WebSocketDestinations;
import com.keystone.dto.NotificationResponse;
import com.keystone.entity.Notification;
import com.keystone.entity.User;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import com.keystone.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationPublisher notificationPublisher;

    private User alice;
    private Notification notification;

    @BeforeEach
    void setUp() {
        alice = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Tech")
                .userEmail("alice@keystone.com")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();
        notification = Notification.builder()
                .id(10L)
                .recipient(alice)
                .type(NotificationType.WORK_ORDER_ASSIGNED)
                .title("Work order assigned")
                .message("You have been assigned work order WO-000123.")
                .relatedEntityType(RelatedEntityType.WORK_ORDER)
                .relatedEntityId(123L)
                .read(false)
                .createdAt(LocalDateTime.of(2026, 10, 3, 12, 0))
                .build();
    }

    @Test
    void publishSafely_ShouldSendOnlyToRecipientUserDestination() {
        notificationPublisher.publishSafely(notification);

        ArgumentCaptor<NotificationResponse> payloadCaptor = ArgumentCaptor.forClass(NotificationResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("alice@keystone.com"),
                eq(WebSocketDestinations.USER_QUEUE_NOTIFICATIONS),
                payloadCaptor.capture()
        );
        NotificationResponse payload = payloadCaptor.getValue();
        assertEquals(10L, payload.getId());
        assertEquals(NotificationType.WORK_ORDER_ASSIGNED, payload.getType());
        assertEquals("Work order assigned", payload.getTitle());
    }

    @Test
    void publishSafely_WhenBrokerFails_ShouldNotThrow() {
        doThrow(new RuntimeException("broker unavailable"))
                .when(messagingTemplate)
                .convertAndSendToUser(eq("alice@keystone.com"), eq(WebSocketDestinations.USER_QUEUE_NOTIFICATIONS), org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(() -> notificationPublisher.publishSafely(notification));
    }

    @Test
    void publishSafely_WhenRecipientMissing_ShouldSkip() {
        notification.setRecipient(null);

        notificationPublisher.publishSafely(notification);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void publishSafely_ShouldNotBroadcastOnSharedTopic() {
        notificationPublisher.publishSafely(notification);

        verify(messagingTemplate).convertAndSendToUser(
                eq("alice@keystone.com"),
                eq("/queue/notifications"),
                org.mockito.ArgumentMatchers.any(NotificationResponse.class)
        );
    }
}
