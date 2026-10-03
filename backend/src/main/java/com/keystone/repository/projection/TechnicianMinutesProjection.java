package com.keystone.repository.projection;

public interface TechnicianMinutesProjection {

    Long getTechnicianId();

    String getFirstName();

    String getLastName();

    long getTotalMinutes();
}
