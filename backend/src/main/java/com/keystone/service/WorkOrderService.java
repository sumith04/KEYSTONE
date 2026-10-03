package com.keystone.service;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.UserResponse;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;

import java.util.List;

public interface WorkOrderService {

    WorkOrderPageResponse getWorkOrders(
            int page,
            int size,
            String sort,
            String search,
            WorkOrderStatus status,
            WorkOrderPriority priority,
            Long customerId,
            Long siteId,
            Long technicianId
    );

    WorkOrderResponse getWorkOrderById(Long id);

    WorkOrderResponse getWorkOrderByNumber(String workOrderNumber);

    WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request, String currentUsername);

    WorkOrderResponse updateWorkOrder(Long id, UpdateWorkOrderRequest request);

    WorkOrderResponse assignWorkOrder(Long id, AssignWorkOrderRequest request);

    WorkOrderResponse startWorkOrder(Long id);

    WorkOrderResponse holdWorkOrder(Long id);

    WorkOrderResponse resumeWorkOrder(Long id);

    WorkOrderResponse completeWorkOrder(Long id);

    WorkOrderResponse closeWorkOrder(Long id);

    WorkOrderResponse cancelWorkOrder(Long id);

    void deleteWorkOrder(Long id);

    List<UserResponse> getAssignableTechnicians();
}
