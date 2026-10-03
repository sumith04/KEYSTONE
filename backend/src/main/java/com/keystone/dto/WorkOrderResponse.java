package com.keystone.dto;

import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.enums.WorkType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderResponse {

    private Long id;
    private String workOrderNumber;
    private String title;
    private String description;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long siteId;
    private String siteCode;
    private String siteName;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private String assignedTechnicianEmail;
    private WorkOrderStatus status;
    private WorkOrderPriority priority;
    private WorkType workType;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private String notes;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkOrderResponse fromEntity(WorkOrder workOrder) {
        if (workOrder == null) {
            return null;
        }

        Customer customer = workOrder.getCustomer();
        Site site = workOrder.getSite();
        User technician = workOrder.getAssignedTechnician();
        User createdBy = workOrder.getCreatedBy();

        return WorkOrderResponse.builder()
                .id(workOrder.getId())
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .title(workOrder.getTitle())
                .description(workOrder.getDescription())
                .customerId(customer != null ? customer.getId() : null)
                .customerCode(customer != null ? customer.getCustomerCode() : null)
                .customerName(customer != null ? customer.getCompanyName() : null)
                .siteId(site != null ? site.getId() : null)
                .siteCode(site != null ? site.getSiteCode() : null)
                .siteName(site != null ? site.getSiteName() : null)
                .assignedTechnicianId(technician != null ? technician.getId() : null)
                .assignedTechnicianName(formatUserName(technician))
                .assignedTechnicianEmail(technician != null ? technician.getUserEmail() : null)
                .status(workOrder.getStatus())
                .priority(workOrder.getPriority())
                .workType(workOrder.getWorkType())
                .scheduledStart(workOrder.getScheduledStart())
                .scheduledEnd(workOrder.getScheduledEnd())
                .actualStart(workOrder.getActualStart())
                .actualEnd(workOrder.getActualEnd())
                .notes(workOrder.getNotes())
                .createdById(createdBy != null ? createdBy.getId() : null)
                .createdByName(formatUserName(createdBy))
                .createdAt(workOrder.getCreatedAt())
                .updatedAt(workOrder.getUpdatedAt())
                .build();
    }

    private static String formatUserName(User user) {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserEmail() : fullName;
    }
}
