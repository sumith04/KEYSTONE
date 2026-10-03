package com.keystone.repository.projection;

public interface WorkOrderMinutesProjection {

    Long getWorkOrderId();

    String getWorkOrderNumber();

    String getTitle();

    long getTotalMinutes();
}
