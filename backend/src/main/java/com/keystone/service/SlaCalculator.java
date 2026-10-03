package com.keystone.service;

import com.keystone.dto.WorkOrderSlaResponse;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.SlaStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public final class SlaCalculator {

    public static final double AT_RISK_REMAINING_FRACTION = 0.20;

    private SlaCalculator() {
    }

    public static boolean hasSla(WorkOrder workOrder) {
        return workOrder != null
                && (workOrder.getSlaResponseDueAt() != null || workOrder.getSlaResolutionDueAt() != null);
    }

    public static SlaStatus calculateStatus(WorkOrder workOrder, LocalDateTime now) {
        if (!hasSla(workOrder)) {
            return SlaStatus.NO_SLA;
        }
        LocalDateTime clock = now != null ? now : LocalDateTime.now();

        boolean responseBreached = isResponseBreached(workOrder, clock);
        boolean resolutionBreached = isResolutionBreached(workOrder, clock);

        if (workOrder.getResolvedAt() != null) {
            return (responseBreached || resolutionBreached) ? SlaStatus.BREACHED : SlaStatus.RESOLVED;
        }
        if (responseBreached || resolutionBreached) {
            return SlaStatus.BREACHED;
        }
        if (isApproaching(workOrder.getCreatedAt(), workOrder.getSlaResponseDueAt(), workOrder.getResponseAt(), clock)
                || isApproaching(workOrder.getCreatedAt(), workOrder.getSlaResolutionDueAt(), workOrder.getResolvedAt(), clock)) {
            return SlaStatus.AT_RISK;
        }
        return SlaStatus.ON_TRACK;
    }

    public static boolean isResponseBreached(WorkOrder workOrder, LocalDateTime now) {
        if (workOrder == null || workOrder.getSlaResponseDueAt() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(workOrder.getResponseBreached())) {
            return true;
        }
        LocalDateTime measured = workOrder.getResponseAt() != null ? workOrder.getResponseAt() : now;
        return measured != null && measured.isAfter(workOrder.getSlaResponseDueAt());
    }

    public static boolean isResolutionBreached(WorkOrder workOrder, LocalDateTime now) {
        if (workOrder == null || workOrder.getSlaResolutionDueAt() == null) {
            return false;
        }
        if (Boolean.TRUE.equals(workOrder.getResolutionBreached())) {
            return true;
        }
        LocalDateTime measured = workOrder.getResolvedAt() != null ? workOrder.getResolvedAt() : now;
        return measured != null && measured.isAfter(workOrder.getSlaResolutionDueAt());
    }

    public static Integer remainingMinutes(LocalDateTime dueAt, LocalDateTime completedAt, LocalDateTime now) {
        if (dueAt == null || completedAt != null) {
            return null;
        }
        LocalDateTime clock = now != null ? now : LocalDateTime.now();
        return (int) Duration.between(clock, dueAt).toMinutes();
    }

    public static WorkOrderSlaResponse toResponse(WorkOrder workOrder, LocalDateTime now) {
        LocalDateTime clock = now != null ? now : LocalDateTime.now();
        return WorkOrderSlaResponse.builder()
                .workOrderId(workOrder.getId())
                .workOrderNumber(workOrder.getWorkOrderNumber())
                .title(workOrder.getTitle())
                .slaPolicyName(workOrder.getSlaPolicyName())
                .slaStatus(calculateStatus(workOrder, clock))
                .responseDueAt(workOrder.getSlaResponseDueAt())
                .responseAt(workOrder.getResponseAt())
                .responseBreached(hasSla(workOrder) ? isResponseBreached(workOrder, clock) : null)
                .resolutionDueAt(workOrder.getSlaResolutionDueAt())
                .resolvedAt(workOrder.getResolvedAt())
                .resolutionBreached(hasSla(workOrder) ? isResolutionBreached(workOrder, clock) : null)
                .remainingResponseMinutes(remainingMinutes(workOrder.getSlaResponseDueAt(), workOrder.getResponseAt(), clock))
                .remainingResolutionMinutes(remainingMinutes(workOrder.getSlaResolutionDueAt(), workOrder.getResolvedAt(), clock))
                .build();
    }

    static boolean isApproaching(LocalDateTime start, LocalDateTime due, LocalDateTime completed, LocalDateTime now) {
        if (completed != null || start == null || due == null || now == null || !due.isAfter(start)) {
            return false;
        }
        if (!now.isBefore(due)) {
            return false;
        }
        long windowMinutes = Duration.between(start, due).toMinutes();
        if (windowMinutes <= 0) {
            return false;
        }
        long remainingMinutes = Duration.between(now, due).toMinutes();
        return remainingMinutes <= Math.round(windowMinutes * AT_RISK_REMAINING_FRACTION);
    }
}
