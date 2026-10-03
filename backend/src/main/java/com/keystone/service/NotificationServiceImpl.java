package com.keystone.service;

import com.keystone.dto.NotificationPageResponse;
import com.keystone.dto.NotificationResponse;
import com.keystone.dto.UnreadCountResponse;
import com.keystone.entity.Notification;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import com.keystone.enums.Role;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.NotificationRepository;
import com.keystone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WorkOrderAccessGuard workOrderAccessGuard;
    private final NotificationCreator notificationCreator;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public NotificationResponse createNotification(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            RelatedEntityType relatedEntityType,
            Long relatedEntityId) {

        Notification saved = notificationCreator.persist(
                recipientId, type, title, message, relatedEntityType, relatedEntityId);
        publishAfterPersist(saved);
        return NotificationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getNotifications(
            int page,
            int size,
            Boolean read,
            NotificationType type,
            String currentUsername) {

        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<Notification> result = notificationRepository.searchForRecipient(
                currentUser.getId(), read, type, pageable);

        return NotificationPageResponse.builder()
                .content(result.getContent().stream().map(NotificationResponse::fromEntity).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .unreadCount(notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        return UnreadCountResponse.builder()
                .unreadCount(notificationRepository.countByRecipientIdAndReadFalse(currentUser.getId()))
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id, String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        Notification notification = notificationRepository.findByIdAndRecipientId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        notificationRepository.markAllReadForRecipient(currentUser.getId(), LocalDateTime.now());
    }

    @Override
    public void notifyWorkOrderAssigned(User technician, WorkOrder workOrder) {
        if (technician == null || workOrder == null) {
            return;
        }
        String number = workOrderNumber(workOrder);
        createSafely(
                technician.getId(),
                NotificationType.WORK_ORDER_ASSIGNED,
                "Work order assigned",
                "You have been assigned work order " + number + ".",
                RelatedEntityType.WORK_ORDER,
                workOrder.getId()
        );
    }

    @Override
    public void notifyWorkOrderStatusChanged(WorkOrder workOrder, WorkOrderStatus status) {
        if (workOrder == null || status == null) {
            return;
        }
        String number = workOrderNumber(workOrder);
        NotificationType type = switch (status) {
            case COMPLETED -> NotificationType.WORK_ORDER_COMPLETED;
            case CLOSED -> NotificationType.WORK_ORDER_CLOSED;
            case CANCELLED -> NotificationType.WORK_ORDER_CANCELLED;
            default -> NotificationType.WORK_ORDER_STATUS_CHANGED;
        };
        String title = switch (status) {
            case COMPLETED -> "Work order completed";
            case CLOSED -> "Work order closed";
            case CANCELLED -> "Work order cancelled";
            default -> "Work order status changed";
        };
        String message = "Work order " + number + " is now " + status.name().replace('_', ' ') + ".";

        if (workOrder.getAssignedTechnician() != null) {
            createSafely(
                    workOrder.getAssignedTechnician().getId(),
                    type,
                    title,
                    message,
                    RelatedEntityType.WORK_ORDER,
                    workOrder.getId()
            );
        }

        if (status == WorkOrderStatus.COMPLETED || status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.CANCELLED) {
            Long technicianId = workOrder.getAssignedTechnician() != null
                    ? workOrder.getAssignedTechnician().getId()
                    : null;
            for (User user : operationalUsers()) {
                if (technicianId != null && technicianId.equals(user.getId())) {
                    continue;
                }
                createSafely(
                        user.getId(),
                        type,
                        title,
                        message,
                        RelatedEntityType.WORK_ORDER,
                        workOrder.getId()
                );
            }
        }
    }

    @Override
    public void notifyLowStockIfNeeded(Part part) {
        if (part == null || part.getQuantityInStock() == null || part.getReorderLevel() == null) {
            return;
        }
        if (part.getQuantityInStock() > part.getReorderLevel()) {
            return;
        }
        String partLabel = part.getPartNumber() != null ? part.getPartNumber() : "part";
        for (User user : operationalUsers()) {
            boolean alreadyUnread = notificationRepository
                    .existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityIdAndReadFalse(
                            user.getId(),
                            NotificationType.PART_LOW_STOCK,
                            RelatedEntityType.PART,
                            part.getId()
                    );
            if (alreadyUnread) {
                continue;
            }
            createSafely(
                    user.getId(),
                    NotificationType.PART_LOW_STOCK,
                    "Part low stock",
                    partLabel + " is at or below reorder level (" + part.getQuantityInStock() + " remaining).",
                    RelatedEntityType.PART,
                    part.getId()
            );
        }
    }

    @Override
    public void notifySlaIfNeeded(WorkOrder workOrder) {
        if (workOrder == null || !SlaCalculator.hasSla(workOrder)) {
            return;
        }
        SlaStatus status = SlaCalculator.calculateStatus(workOrder, LocalDateTime.now());
        NotificationType type;
        String title;
        if (status == SlaStatus.BREACHED) {
            type = NotificationType.SLA_BREACHED;
            title = "SLA breached";
        } else if (status == SlaStatus.AT_RISK) {
            type = NotificationType.SLA_AT_RISK;
            title = "SLA at risk";
        } else {
            return;
        }

        String number = workOrderNumber(workOrder);
        String message = "Work order " + number + " is " + status.name().replace('_', ' ') + ".";
        List<User> recipients = new ArrayList<>();
        if (workOrder.getAssignedTechnician() != null) {
            recipients.add(workOrder.getAssignedTechnician());
        }
        recipients.addAll(operationalUsers());

        for (User user : recipients) {
            boolean exists = notificationRepository.existsByRecipientIdAndTypeAndRelatedEntityTypeAndRelatedEntityId(
                    user.getId(),
                    type,
                    RelatedEntityType.WORK_ORDER,
                    workOrder.getId()
            );
            if (exists) {
                continue;
            }
            createSafely(
                    user.getId(),
                    type,
                    title,
                    message,
                    RelatedEntityType.WORK_ORDER,
                    workOrder.getId()
            );
        }
    }

    private void createSafely(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            RelatedEntityType relatedEntityType,
            Long relatedEntityId) {
        try {
            Notification saved = notificationCreator.persist(
                    recipientId, type, title, message, relatedEntityType, relatedEntityId);
            publishAfterPersist(saved);
        } catch (Exception ex) {
            log.warn("Failed to create {} notification for user {}: {}", type, recipientId, ex.getMessage());
        }
    }

    private void publishAfterPersist(Notification saved) {
        try {
            notificationPublisher.publishSafely(saved);
        } catch (Exception ex) {
            log.warn("WebSocket publish failed for notification {}: {}", saved != null ? saved.getId() : null, ex.getMessage());
        }
    }

    private List<User> operationalUsers() {
        List<User> users = new ArrayList<>();
        users.addAll(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.ADMIN, true));
        users.addAll(userRepository.findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role.MANAGER, true));
        return users;
    }

    private String workOrderNumber(WorkOrder workOrder) {
        return workOrder.getWorkOrderNumber() != null ? workOrder.getWorkOrderNumber() : "a work order";
    }
}
