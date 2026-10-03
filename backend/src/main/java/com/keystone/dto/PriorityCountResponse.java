package com.keystone.dto;

import com.keystone.enums.WorkOrderPriority;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityCountResponse {

    private WorkOrderPriority priority;
    private long count;
}
