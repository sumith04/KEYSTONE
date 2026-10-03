package com.keystone.repository.projection;

import com.keystone.enums.WorkOrderPriority;

public interface PriorityCountProjection {

    WorkOrderPriority getPriority();

    long getTotal();
}
