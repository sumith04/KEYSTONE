package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPortalSummaryResponse {

    private long submittedRequests;
    private long openRequests;
    private long activeWorkOrders;
    private long completedWorkOrders;
    private long slaWarnings;
}
