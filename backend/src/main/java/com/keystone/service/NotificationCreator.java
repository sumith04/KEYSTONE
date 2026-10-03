package com.keystone.service;

import com.keystone.entity.Notification;
import com.keystone.entity.User;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.NotificationRepository;
import com.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationCreator {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Persists a notification in its own transaction so a failure cannot mark
     * an outer work-order or inventory transaction rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification persist(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            RelatedEntityType relatedEntityType,
            Long relatedEntityId) {

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + recipientId));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .read(false)
                .build();

        return notificationRepository.save(notification);
    }
}
