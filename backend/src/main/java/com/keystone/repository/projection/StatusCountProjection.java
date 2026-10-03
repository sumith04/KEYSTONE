package com.keystone.repository.projection;

import com.keystone.enums.WorkOrderStatus;

public interface StatusCountProjection {

    WorkOrderStatus getStatus();

    long getTotal();
}
