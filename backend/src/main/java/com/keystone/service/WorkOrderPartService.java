package com.keystone.service;

import com.keystone.dto.CreateWorkOrderPartRequest;
import com.keystone.dto.UpdateWorkOrderPartRequest;
import com.keystone.dto.WorkOrderPartResponse;

import java.util.List;

public interface WorkOrderPartService {

    List<WorkOrderPartResponse> getWorkOrderParts(Long workOrderId, String currentUsername);

    WorkOrderPartResponse addWorkOrderPart(Long workOrderId, CreateWorkOrderPartRequest request, String currentUsername);

    WorkOrderPartResponse updateWorkOrderPart(
            Long workOrderId,
            Long workOrderPartId,
            UpdateWorkOrderPartRequest request,
            String currentUsername
    );

    void deleteWorkOrderPart(Long workOrderId, Long workOrderPartId, String currentUsername);
}
