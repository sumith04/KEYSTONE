package com.keystone.dto;

import com.keystone.enums.SlaStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderSlaResponse {

    private Long workOrderId;
    private String workOrderNumber;
    private String title;
    private String slaPolicyName;
    private SlaStatus slaStatus;
    private LocalDateTime responseDueAt;
    private LocalDateTime responseAt;
    private Boolean responseBreached;
    private LocalDateTime resolutionDueAt;
    private LocalDateTime resolvedAt;
    private Boolean resolutionBreached;
    private Integer remainingResponseMinutes;
    private Integer remainingResolutionMinutes;
}
