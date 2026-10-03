package com.keystone.service;

import com.keystone.config.WebSocketDestinations;
import com.keystone.dto.NotificationResponse;
import com.keystone.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Best-effort user-specific delivery. Failures are logged and never thrown
     * so a broker outage cannot undo a persisted notification.
     */
    public void publishSafely(Notification notification) {
        if (notification == null || notification.getRecipient() == null
                || notification.getRecipient().getUserEmail() == null
                || notification.getRecipient().getUserEmail().isBlank()) {
            return;
        }

        try {
            String username = notification.getRecipient().getUserEmail();
            messagingTemplate.convertAndSendToUser(
                    username,
                    WebSocketDestinations.USER_QUEUE_NOTIFICATIONS,
                    NotificationResponse.fromEntity(notification)
            );
            log.debug("Published notification {} to user destination", notification.getId());
        } catch (Exception ex) {
            log.warn("WebSocket delivery failed for notification {}: {}", notification.getId(), ex.getMessage());
        }
    }
}
