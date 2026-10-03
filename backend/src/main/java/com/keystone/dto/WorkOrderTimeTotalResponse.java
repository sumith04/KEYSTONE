package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderTimeTotalResponse {

    private Long workOrderId;
    private String workOrderNumber;
    private String title;
    private long totalLoggedMinutes;
}
