package com.keystone.dto;

import com.keystone.enums.WorkOrderStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusCountResponse {

    private WorkOrderStatus status;
    private long count;
}
