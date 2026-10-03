package com.keystone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderSummaryResponse {

    private long assigned;
    private long inProgress;
    private long onHold;
    private long completed;
}
