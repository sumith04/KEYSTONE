package com.keystone.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaDashboardResponse {

    private long onTrack;
    private long atRisk;
    private long breached;
    private long resolved;
    private BigDecimal slaCompliancePercentage;
}
