package com.keystone.repository.projection;

import com.keystone.enums.WorkOrderStatus;

public interface TechnicianStatusCountProjection {

    Long getTechnicianId();

    String getFirstName();

    String getLastName();

    WorkOrderStatus getStatus();

    long getTotal();
}
