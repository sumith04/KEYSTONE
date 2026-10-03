package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianPerformanceResponse {

    private Long technicianId;
    private String technicianName;
    private long assignedCount;
    private long completedCount;
    private long closedCount;
    private long completedWithinSla;
    private long slaBreaches;
    private long totalLoggedMinutes;
}
