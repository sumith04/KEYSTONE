package com.keystone.service;

import com.keystone.entity.WorkOrder;
import com.keystone.enums.SlaStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public final class SlaMetrics {

    private final long onTrack;
    private final long atRisk;
    private final long breached;
    private final long resolved;
    private final long noSla;
    private final long withSla;
    private final long resolvedWithSla;
    private final long resolvedWithinSla;
    private final long responseBreaches;
    private final long resolutionBreaches;

    private SlaMetrics(
            long onTrack,
            long atRisk,
            long breached,
            long resolved,
            long noSla,
            long withSla,
            long resolvedWithSla,
            long resolvedWithinSla,
            long responseBreaches,
            long resolutionBreaches) {
        this.onTrack = onTrack;
        this.atRisk = atRisk;
        this.breached = breached;
        this.resolved = resolved;
        this.noSla = noSla;
        this.withSla = withSla;
        this.resolvedWithSla = resolvedWithSla;
        this.resolvedWithinSla = resolvedWithinSla;
        this.responseBreaches = responseBreaches;
        this.resolutionBreaches = resolutionBreaches;
    }

    /**
     * SLA status is calculated dynamically and is not stored. Callers should load only
     * work orders that already have snapshot deadlines so this loop does not scan the
     * entire catalog.
     */
    public static SlaMetrics from(List<WorkOrder> workOrders, LocalDateTime now) {
        long onTrack = 0;
        long atRisk = 0;
        long breached = 0;
        long resolved = 0;
        long noSla = 0;
        long withSla = 0;
        long resolvedWithSla = 0;
        long resolvedWithinSla = 0;
        long responseBreaches = 0;
        long resolutionBreaches = 0;
        LocalDateTime clock = now != null ? now : LocalDateTime.now();

        for (WorkOrder workOrder : workOrders) {
            if (!SlaCalculator.hasSla(workOrder)) {
                noSla++;
                continue;
            }
            withSla++;
            SlaStatus status = SlaCalculator.calculateStatus(workOrder, clock);
            switch (status) {
                case ON_TRACK -> onTrack++;
                case AT_RISK -> atRisk++;
                case BREACHED -> breached++;
                case RESOLVED -> resolved++;
                case NO_SLA -> noSla++;
            }
            if (workOrder.getResolvedAt() != null) {
                resolvedWithSla++;
                if (status == SlaStatus.RESOLVED) {
                    resolvedWithinSla++;
                }
            }
            if (SlaCalculator.isResponseBreached(workOrder, clock)) {
                responseBreaches++;
            }
            if (SlaCalculator.isResolutionBreached(workOrder, clock)) {
                resolutionBreaches++;
            }
        }

        return new SlaMetrics(
                onTrack, atRisk, breached, resolved, noSla, withSla,
                resolvedWithSla, resolvedWithinSla, responseBreaches, resolutionBreaches
        );
    }

    public long getOnTrack() {
        return onTrack;
    }

    public long getAtRisk() {
        return atRisk;
    }

    public long getBreached() {
        return breached;
    }

    public long getResolved() {
        return resolved;
    }

    public long getNoSla() {
        return noSla;
    }

    public long getWithSla() {
        return withSla;
    }

    public long getResolvedWithSla() {
        return resolvedWithSla;
    }

    public long getResolvedWithinSla() {
        return resolvedWithinSla;
    }

    public long getResponseBreaches() {
        return responseBreaches;
    }

    public long getResolutionBreaches() {
        return resolutionBreaches;
    }

    /**
     * Compliance is resolved-within-SLA divided by resolved work orders that had an SLA.
     * Returns null when the denominator is zero.
     */
    public BigDecimal compliancePercentage() {
        if (resolvedWithSla == 0) {
            return null;
        }
        return BigDecimal.valueOf(resolvedWithinSla)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(resolvedWithSla), 1, RoundingMode.HALF_UP);
    }
}
