package com.keystone.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeReportResponse {

    private long totalLoggedMinutes;
    private BigDecimal totalLoggedHours;
    private long logsCount;
    private List<TechnicianTimeTotalResponse> minutesByTechnician;
    private List<WorkOrderTimeTotalResponse> minutesByWorkOrder;
}
