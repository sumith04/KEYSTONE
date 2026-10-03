package com.keystone.dto;

import com.keystone.entity.ServiceRequest;
import com.keystone.entity.Site;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.enums.WorkType;
import com.keystone.service.SlaCalculator;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerWorkOrderResponse {

    private Long id;
    private String workOrderNumber;
    private String title;
    private String description;
    private Long siteId;
    private String siteCode;
    private String siteName;
    private String assignedTechnicianName;
    private WorkOrderStatus status;
    private WorkOrderPriority priority;
    private WorkType workType;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private SlaStatus slaStatus;
    private LocalDateTime responseDueAt;
    private LocalDateTime responseAt;
    private LocalDateTime resolutionDueAt;
    private LocalDateTime resolvedAt;
    private Long serviceRequestId;
    private String serviceRequestNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerWorkOrderResponse fromEntity(WorkOrder workOrder, ServiceRequest serviceRequest) {
        if (workOrder == null) {
            return null;
        }
        Site site = workOrder.getSite();
        User technician = workOrder.getAssignedTechnician();
        return CustomerWorkOrderResponse.builder()
                .id(workOrder.getId())
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .title(workOrder.getTitle())
                .description(workOrder.getDescription())
                .siteId(site != null ? site.getId() : null)
                .siteCode(site != null ? site.getSiteCode() : null)
                .siteName(site != null ? site.getSiteName() : null)
                .assignedTechnicianName(formatName(technician))
                .status(workOrder.getStatus())
                .priority(workOrder.getPriority())
                .workType(workOrder.getWorkType())
                .scheduledStart(workOrder.getScheduledStart())
                .scheduledEnd(workOrder.getScheduledEnd())
                .actualStart(workOrder.getActualStart())
                .actualEnd(workOrder.getActualEnd())
                .slaStatus(SlaCalculator.calculateStatus(workOrder, LocalDateTime.now()))
                .responseDueAt(workOrder.getSlaResponseDueAt())
                .responseAt(workOrder.getResponseAt())
                .resolutionDueAt(workOrder.getSlaResolutionDueAt())
                .resolvedAt(workOrder.getResolvedAt())
                .serviceRequestId(serviceRequest != null ? serviceRequest.getId() : null)
                .serviceRequestNumber(serviceRequest != null ? serviceRequest.getRequestNumber() : null)
                .createdAt(workOrder.getCreatedAt())
                .updatedAt(workOrder.getUpdatedAt())
                .build();
    }

    private static String formatName(User user) {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? null : fullName;
    }
}
