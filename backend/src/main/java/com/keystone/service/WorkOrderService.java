package com.keystone.service;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.UserResponse;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.dto.WorkOrderSlaPageResponse;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.dto.WorkOrderSummaryResponse;
import com.keystone.enums.SlaStatus;
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
            Long technicianId,
            SlaStatus slaStatus,
            String currentUsername
    );

    WorkOrderSummaryResponse getWorkOrderSummary(String currentUsername);

    WorkOrderResponse getWorkOrderById(Long id, String currentUsername);

    WorkOrderResponse getWorkOrderByNumber(String workOrderNumber, String currentUsername);

    WorkOrderResponse createWorkOrder(CreateWorkOrderRequest request, String currentUsername);

    WorkOrderResponse updateWorkOrder(Long id, UpdateWorkOrderRequest request, String currentUsername);

    WorkOrderResponse assignWorkOrder(Long id, AssignWorkOrderRequest request, String currentUsername);

    WorkOrderResponse startWorkOrder(Long id, String currentUsername);

    WorkOrderResponse holdWorkOrder(Long id, String currentUsername);

    WorkOrderResponse resumeWorkOrder(Long id, String currentUsername);

    WorkOrderResponse completeWorkOrder(Long id, String currentUsername);

    WorkOrderResponse closeWorkOrder(Long id, String currentUsername);

    WorkOrderResponse cancelWorkOrder(Long id, String currentUsername);

    void deleteWorkOrder(Long id, String currentUsername);

    List<UserResponse> getAssignableTechnicians();

    WorkOrderSlaResponse getWorkOrderSla(Long id, String currentUsername);

    WorkOrderSlaPageResponse getWorkOrderSlaList(
            int page,
            int size,
            String sort,
            String search,
            WorkOrderStatus status,
            WorkOrderPriority priority,
            Long customerId,
            Long siteId,
            Long technicianId,
            SlaStatus slaStatus,
            String currentUsername
    );
}
