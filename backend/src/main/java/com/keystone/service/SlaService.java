package com.keystone.service;

import com.keystone.dto.CreateSlaPolicyRequest;
import com.keystone.dto.SlaPolicyPageResponse;
import com.keystone.dto.SlaPolicyResponse;
import com.keystone.dto.UpdateSlaPolicyRequest;
import com.keystone.dto.WorkOrderSlaPageResponse;
import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;

import java.time.LocalDateTime;

public interface SlaService {

    SlaPolicyPageResponse getPolicies(int page, int size, String sort, String search, WorkOrderPriority priority, Boolean active);

    SlaPolicyResponse getPolicyById(Long id);

    SlaPolicyResponse createPolicy(CreateSlaPolicyRequest request);

    SlaPolicyResponse updatePolicy(Long id, UpdateSlaPolicyRequest request);

    void deletePolicy(Long id);

    void applySnapshot(WorkOrder workOrder, Customer customer);

    void recordResponseIfNeeded(WorkOrder workOrder, LocalDateTime at);

    void recordResolutionIfNeeded(WorkOrder workOrder, LocalDateTime at);

    WorkOrderSlaResponse getWorkOrderSla(Long workOrderId, String currentUsername);

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
