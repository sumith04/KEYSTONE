package com.keystone.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private long totalWorkOrders;
    private long newWorkOrders;
    private long assignedWorkOrders;
    private long inProgressWorkOrders;
    private long onHoldWorkOrders;
    private long completedWorkOrders;
    private long closedWorkOrders;
    private long cancelledWorkOrders;
    private long openWorkOrders;
    private long slaBreachedWorkOrders;
    private long slaAtRiskWorkOrders;
    private long activeTechnicians;
    private long lowStockParts;
    private long totalParts;
    private List<StatusCountResponse> statusDistribution;
    private List<PriorityCountResponse> priorityDistribution;
    private List<TechnicianWorkloadResponse> technicianWorkload;
    private SlaDashboardResponse sla;
    private InventoryDashboardResponse inventory;
    private TimeDashboardResponse time;
}
