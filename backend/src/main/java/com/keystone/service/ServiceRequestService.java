package com.keystone.service;

import com.keystone.dto.CreateServiceRequest;
import com.keystone.dto.ServiceRequestPageResponse;
import com.keystone.dto.ServiceRequestResponse;
import com.keystone.dto.UpdateServiceRequest;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;

public interface ServiceRequestService {

    ServiceRequestPageResponse getMyRequests(
            int page,
            int size,
            String sort,
            String search,
            ServiceRequestStatus status,
            WorkOrderPriority priority,
            String currentUsername
    );

    ServiceRequestResponse getMyRequest(Long id, String currentUsername);

    ServiceRequestResponse createMyRequest(CreateServiceRequest request, String currentUsername);

    ServiceRequestResponse updateMyRequest(Long id, UpdateServiceRequest request, String currentUsername);

    ServiceRequestResponse cancelMyRequest(Long id, String currentUsername);

    ServiceRequestPageResponse getRequests(
            int page,
            int size,
            String sort,
            String search,
            ServiceRequestStatus status,
            WorkOrderPriority priority,
            Long customerId
    );

    ServiceRequestResponse getRequest(Long id);

    ServiceRequestResponse acknowledge(Long id, String currentUsername);

    ServiceRequestResponse markInReview(Long id, String currentUsername);

    ServiceRequestResponse reject(Long id, String currentUsername);

    ServiceRequestResponse convertToWorkOrder(Long id, String currentUsername);
}
