package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDashboardResponse {

    private long totalParts;
    private long activeParts;
    private long lowStockParts;
    private long outOfStockParts;
}
