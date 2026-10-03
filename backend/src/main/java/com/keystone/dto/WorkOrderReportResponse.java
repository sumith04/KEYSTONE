package com.keystone.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderReportResponse {

    private long total;
    private long created;
    private long completed;
    private long closed;
    private long cancelled;
    private Long averageCompletionMinutes;
    private List<StatusCountResponse> statusDistribution;
    private List<PriorityCountResponse> priorityDistribution;
}
