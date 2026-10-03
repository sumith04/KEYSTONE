package com.keystone.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaReportResponse {

    private long totalWorkOrdersWithSla;
    private long resolvedWithinSla;
    private long breached;
    private long atRisk;
    private long onTrack;
    private long resolved;
    private BigDecimal slaCompliancePercentage;
    private long responseSlaBreaches;
    private long resolutionSlaBreaches;
}
