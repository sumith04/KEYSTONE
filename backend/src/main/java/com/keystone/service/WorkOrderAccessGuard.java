package com.keystone.service;

import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class WorkOrderAccessGuard {

    private static final Set<WorkOrderStatus> PARTS_ALLOWED_STATUSES = EnumSet.of(
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.ON_HOLD
    );

    private static final Set<WorkOrderStatus> TIME_LOG_ALLOWED_STATUSES = EnumSet.of(
            WorkOrderStatus.NEW,
            WorkOrderStatus.ASSIGNED,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.ON_HOLD,
            WorkOrderStatus.COMPLETED
    );

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;

    public User requireCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ApiException("Authenticated user could not be resolved.", HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByUserEmail(username.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    public WorkOrder requireAccessibleWorkOrder(Long workOrderId, String currentUsername) {
        WorkOrder workOrder = workOrderRepository.findByIdWithRelations(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + workOrderId));
        assertTechnicianOwnsWorkOrder(workOrder, currentUsername, "Work order not found with id: " + workOrderId);
        return workOrder;
    }

    public void assertTechnicianOwnsWorkOrder(WorkOrder workOrder, String currentUsername, String notFoundMessage) {
        User currentUser = requireCurrentUser(currentUsername);
        if (!isTechnician(currentUser)) {
            return;
        }
        User assignedTechnician = workOrder.getAssignedTechnician();
        if (assignedTechnician == null || !currentUser.getId().equals(assignedTechnician.getId())) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }

    public void assertPartsMutable(WorkOrder workOrder) {
        WorkOrderStatus status = workOrder.getStatus();
        if (status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.CANCELLED) {
            throw new ApiException(
                    "Parts cannot be consumed on a closed or cancelled work order.",
                    HttpStatus.CONFLICT
            );
        }
        if (!PARTS_ALLOWED_STATUSES.contains(status)) {
            throw new ApiException(
                    "Parts can only be used when the work order is ASSIGNED, IN_PROGRESS, or ON_HOLD.",
                    HttpStatus.CONFLICT
            );
        }
    }

    public void assertTimeLogsMutable(WorkOrder workOrder) {
        WorkOrderStatus status = workOrder.getStatus();
        if (status == WorkOrderStatus.CLOSED || status == WorkOrderStatus.CANCELLED) {
            throw new ApiException(
                    "Time logs cannot be created or changed on a closed or cancelled work order.",
                    HttpStatus.CONFLICT
            );
        }
        if (!TIME_LOG_ALLOWED_STATUSES.contains(status)) {
            throw new ApiException(
                    "Time logs cannot be recorded for this work order status.",
                    HttpStatus.CONFLICT
            );
        }
    }

    public boolean isTechnician(User user) {
        return user != null && user.getRole() == Role.TECHNICIAN;
    }
}
