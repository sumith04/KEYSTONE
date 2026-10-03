package com.keystone.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReportResponse {

    private long totalParts;
    private long activeParts;
    private long inactiveParts;
    private long lowStock;
    private long outOfStock;
    private BigDecimal totalInventoryValue;
}
