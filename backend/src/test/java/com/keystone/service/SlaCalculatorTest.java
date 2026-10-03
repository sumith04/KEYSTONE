package com.keystone.service;

import com.keystone.entity.WorkOrder;
import com.keystone.enums.SlaStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SlaCalculatorTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 3, 8, 0);

    @Test
    void noDueDates_ShouldBeNoSla() {
        WorkOrder workOrder = WorkOrder.builder().createdAt(CREATED).build();
        assertEquals(SlaStatus.NO_SLA, SlaCalculator.calculateStatus(workOrder, CREATED.plusHours(1)));
    }

    @Test
    void earlyInWindow_ShouldBeOnTrack() {
        WorkOrder workOrder = slaWorkOrder(120, 240);
        assertEquals(SlaStatus.ON_TRACK, SlaCalculator.calculateStatus(workOrder, CREATED.plusMinutes(20)));
    }

    @Test
    void finalTwentyPercent_ShouldBeAtRisk() {
        WorkOrder workOrder = slaWorkOrder(100, 200);
        assertEquals(SlaStatus.AT_RISK, SlaCalculator.calculateStatus(workOrder, CREATED.plusMinutes(85)));
    }

    @Test
    void overdueWithoutResponse_ShouldBeBreached() {
        WorkOrder workOrder = slaWorkOrder(60, 180);
        assertEquals(SlaStatus.BREACHED, SlaCalculator.calculateStatus(workOrder, CREATED.plusMinutes(90)));
    }

    @Test
    void completedWithinSla_ShouldBeResolved() {
        WorkOrder workOrder = slaWorkOrder(120, 240);
        workOrder.setResponseAt(CREATED.plusMinutes(30));
        workOrder.setResponseBreached(false);
        workOrder.setResolvedAt(CREATED.plusMinutes(90));
        workOrder.setResolutionBreached(false);
        assertEquals(SlaStatus.RESOLVED, SlaCalculator.calculateStatus(workOrder, CREATED.plusHours(5)));
    }

    @Test
    void completedLate_ShouldStayBreached() {
        WorkOrder workOrder = slaWorkOrder(60, 90);
        workOrder.setResponseAt(CREATED.plusMinutes(70));
        workOrder.setResponseBreached(true);
        workOrder.setResolvedAt(CREATED.plusMinutes(100));
        workOrder.setResolutionBreached(true);
        assertEquals(SlaStatus.BREACHED, SlaCalculator.calculateStatus(workOrder, CREATED.plusHours(3)));
    }

    @Test
    void remainingMinutes_ShouldBeServerCalculated() {
        WorkOrder workOrder = slaWorkOrder(120, 240);
        assertEquals(100, SlaCalculator.remainingMinutes(workOrder.getSlaResponseDueAt(), null, CREATED.plusMinutes(20)));
        assertNull(SlaCalculator.remainingMinutes(workOrder.getSlaResponseDueAt(), CREATED.plusMinutes(10), CREATED.plusMinutes(20)));
    }

    private WorkOrder slaWorkOrder(int responseMinutes, int resolutionMinutes) {
        return WorkOrder.builder()
                .createdAt(CREATED)
                .slaPolicyName("Gold")
                .slaResponseDueAt(CREATED.plusMinutes(responseMinutes))
                .slaResolutionDueAt(CREATED.plusMinutes(resolutionMinutes))
                .build();
    }
}
