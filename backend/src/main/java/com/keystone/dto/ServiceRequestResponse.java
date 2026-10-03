package com.keystone.dto;

import com.keystone.entity.Customer;
import com.keystone.entity.ServiceRequest;
import com.keystone.entity.Site;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.ServiceRequestStatus;
import com.keystone.enums.WorkOrderPriority;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRequestResponse {

    private Long id;
    private String requestNumber;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long siteId;
    private String siteCode;
    private String siteName;
    private String title;
    private String description;
    private WorkOrderPriority priority;
    private ServiceRequestStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime preferredDate;
    private String contactName;
    private String contactPhone;
    private Long workOrderId;
    private String workOrderNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ServiceRequestResponse fromEntity(ServiceRequest request) {
        if (request == null) {
            return null;
        }
        Customer customer = request.getCustomer();
        Site site = request.getSite();
        WorkOrder workOrder = request.getWorkOrder();
        return ServiceRequestResponse.builder()
                .id(request.getId())
                .requestNumber(request.getRequestNumber())
                .customerId(customer != null ? customer.getId() : null)
                .customerCode(customer != null ? customer.getCustomerCode() : null)
                .customerName(customer != null ? customer.getCompanyName() : null)
                .siteId(site != null ? site.getId() : null)
                .siteCode(site != null ? site.getSiteCode() : null)
                .siteName(site != null ? site.getSiteName() : null)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(request.getStatus())
                .requestedAt(request.getRequestedAt())
                .preferredDate(request.getPreferredDate())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .workOrderId(workOrder != null ? workOrder.getId() : null)
                .workOrderNumber(workOrder != null ? workOrder.getWorkOrderNumber() : null)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
