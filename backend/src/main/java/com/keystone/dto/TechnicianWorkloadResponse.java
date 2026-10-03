package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianWorkloadResponse {

    private Long technicianId;
    private String technicianName;
    private long assignedWorkOrders;
    private long inProgressWorkOrders;
    private long completedWorkOrders;
    private long overdueSlaWorkOrders;
}
