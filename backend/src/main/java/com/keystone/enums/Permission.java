package com.keystone.enums;

public enum Permission {
    // Authentication & Core
    LOGIN,
    LOGOUT,

    // User Management
    CREATE_USER,
    UPDATE_USER,
    VIEW_USER,
    DELETE_USER,

    // Customer Management
    CREATE_CUSTOMER,
    UPDATE_CUSTOMER,
    VIEW_CUSTOMER,
    DELETE_CUSTOMER,

    // Site / Facility Management
    CREATE_SITE,
    UPDATE_SITE,
    VIEW_SITE,
    DELETE_SITE,

    // Work Order Management
    CREATE_WORK_ORDER,
    UPDATE_WORK_ORDER,
    VIEW_WORK_ORDER,
    DELETE_WORK_ORDER,
    ASSIGN_WORK_ORDER,
    CANCEL_WORK_ORDER,
    CLOSE_WORK_ORDER,

    // Work Execution
    START_WORK,
    HOLD_WORK,
    RESUME_WORK,
    COMPLETE_WORK,

    // Inventory & Parts
    ADD_PART,
    UPDATE_PART,
    DELETE_PART,
    VIEW_PART,
    USE_PARTS,

    // Time Tracking
    ADD_TIME_LOG,
    VIEW_TIME_LOGS,

    // Analytics & Notifications
    VIEW_DASHBOARD,
    VIEW_REPORT,
    SEND_NOTIFICATION,

    // Customer Self-Service
    REQUEST_RAISE,
    VIEW_OWN_REQUEST
}
