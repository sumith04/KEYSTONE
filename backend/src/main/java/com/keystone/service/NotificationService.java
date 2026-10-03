package com.keystone.service;

import com.keystone.dto.NotificationPageResponse;
import com.keystone.dto.NotificationResponse;
import com.keystone.dto.UnreadCountResponse;
import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.NotificationType;
import com.keystone.enums.RelatedEntityType;
import com.keystone.enums.WorkOrderStatus;

public interface NotificationService {

    NotificationResponse createNotification(
            Long recipientId,
            NotificationType type,
            String title,
            String message,
            RelatedEntityType relatedEntityType,
            Long relatedEntityId
    );

    NotificationPageResponse getNotifications(
            int page,
            int size,
            Boolean read,
            NotificationType type,
            String currentUsername
    );

    UnreadCountResponse getUnreadCount(String currentUsername);

    NotificationResponse markAsRead(Long id, String currentUsername);

    void markAllAsRead(String currentUsername);

    void notifyWorkOrderAssigned(User technician, WorkOrder workOrder);

    void notifyWorkOrderStatusChanged(WorkOrder workOrder, WorkOrderStatus status);

    void notifyLowStockIfNeeded(Part part);

    /**
     * Creates SLA_AT_RISK or SLA_BREACHED notifications when the calculated status
     * matches. Deduplicates per recipient/type/work order. Intended for a future
     * scheduled monitor — KEYSTONE has no scheduler yet.
     */
    void notifySlaIfNeeded(WorkOrder workOrder);
}
