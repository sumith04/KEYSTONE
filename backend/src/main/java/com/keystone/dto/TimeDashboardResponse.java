package com.keystone.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeDashboardResponse {

    private long totalLoggedMinutes;
    private BigDecimal totalLoggedHours;
    private long activeTechniciansWithTimeLogs;
}
