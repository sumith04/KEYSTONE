package com.keystone.dto;

import com.keystone.entity.Part;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrderPart;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderPartResponse {

    private Long id;
    private Long workOrderId;
    private Long partId;
    private String partNumber;
    private String partName;
    private Integer quantityUsed;
    private BigDecimal unitCostAtUsage;
    private BigDecimal totalCost;
    private Long usedById;
    private String usedByName;
    private LocalDateTime usedAt;

    public static WorkOrderPartResponse fromEntity(WorkOrderPart usage) {
        if (usage == null) {
            return null;
        }
        Part part = usage.getPart();
        User usedBy = usage.getUsedBy();
        return WorkOrderPartResponse.builder()
                .id(usage.getId())
                .workOrderId(usage.getWorkOrder() != null ? usage.getWorkOrder().getId() : null)
                .partId(part != null ? part.getId() : null)
                .partNumber(part != null ? part.getPartNumber() : null)
                .partName(part != null ? part.getName() : null)
                .quantityUsed(usage.getQuantityUsed())
                .unitCostAtUsage(usage.getUnitCostAtUsage())
                .totalCost(usage.getTotalCost())
                .usedById(usedBy != null ? usedBy.getId() : null)
                .usedByName(formatUserName(usedBy))
                .usedAt(usage.getUsedAt())
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
