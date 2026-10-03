package com.keystone.dto;

import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.service.SlaCalculator;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentWorkOrderResponse {

    private Long id;
    private String workOrderNumber;
    private String title;
    private WorkOrderStatus status;
    private WorkOrderPriority priority;
    private String customer;
    private String site;
    private String technician;
    private LocalDateTime createdAt;
    private SlaStatus slaStatus;

    public static RecentWorkOrderResponse fromEntity(WorkOrder workOrder, LocalDateTime now) {
        if (workOrder == null) {
            return null;
        }
        Customer customer = workOrder.getCustomer();
        Site site = workOrder.getSite();
        User technician = workOrder.getAssignedTechnician();
        return RecentWorkOrderResponse.builder()
                .id(workOrder.getId())
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .title(workOrder.getTitle())
                .status(workOrder.getStatus())
                .priority(workOrder.getPriority())
                .customer(customer != null ? customer.getCompanyName() : null)
                .site(site != null ? site.getSiteName() : null)
                .technician(formatName(technician))
                .createdAt(workOrder.getCreatedAt())
                .slaStatus(SlaCalculator.calculateStatus(workOrder, now))
                .build();
    }

    private static String formatName(User user) {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? user.getUserEmail() : fullName;
    }
}
