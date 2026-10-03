package com.keystone.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderTrendResponse {

    private String period;
    private long created;
    private long completed;
    private long closed;
}
