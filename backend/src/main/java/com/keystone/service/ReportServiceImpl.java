package com.keystone.service;

import com.keystone.dto.InventoryReportResponse;
import com.keystone.dto.PriorityCountResponse;
import com.keystone.dto.SlaReportResponse;
import com.keystone.dto.StatusCountResponse;
import com.keystone.dto.TechnicianPerformanceResponse;
import com.keystone.dto.TechnicianTimeTotalResponse;
import com.keystone.dto.TimeReportResponse;
import com.keystone.dto.WorkOrderReportResponse;
import com.keystone.dto.WorkOrderTimeTotalResponse;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.PartStatus;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.repository.PartRepository;
import com.keystone.repository.TimeLogRepository;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.repository.projection.PriorityCountProjection;
import com.keystone.repository.projection.StatusCountProjection;
import com.keystone.repository.projection.TechnicianMinutesProjection;
import com.keystone.repository.projection.TechnicianStatusCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final WorkOrderRepository workOrderRepository;
    private final PartRepository partRepository;
    private final TimeLogRepository timeLogRepository;
    private final DashboardServiceImpl dashboardService;

    @Override
    @Transactional(readOnly = true)
    public WorkOrderReportResponse getWorkOrderReport(String from, String to, String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parse(from, to);
        DashboardServiceImpl.Scope scope = dashboardService.resolveScope(currentUsername);

        Map<WorkOrderStatus, Long> statusCounts = completeStatusMap(
                workOrderRepository.countByStatusInRange(range.from(), range.to(), scope.technicianId()));
        Map<WorkOrderPriority, Long> priorityCounts = completePriorityMap(
                workOrderRepository.countByPriorityInRange(range.from(), range.to(), scope.technicianId()));
        long total = workOrderRepository.countCreatedInRange(range.from(), range.to(), scope.technicianId());

        List<Object[]> completions = workOrderRepository.findCompletionPairsInRange(
                range.from(), range.to(), scope.technicianId());
        Long averageCompletionMinutes = null;
        if (!completions.isEmpty()) {
            long totalMinutes = 0;
            int counted = 0;
            for (Object[] pair : completions) {
                LocalDateTime createdAt = (LocalDateTime) pair[0];
                LocalDateTime actualEnd = (LocalDateTime) pair[1];
                if (createdAt != null && actualEnd != null && !actualEnd.isBefore(createdAt)) {
                    totalMinutes += Duration.between(createdAt, actualEnd).toMinutes();
                    counted++;
                }
            }
            if (counted > 0) {
                averageCompletionMinutes = totalMinutes / counted;
            }
        }

        return WorkOrderReportResponse.builder()
                .total(total)
                .created(total)
                .completed(statusCounts.get(WorkOrderStatus.COMPLETED))
                .closed(statusCounts.get(WorkOrderStatus.CLOSED))
                .cancelled(statusCounts.get(WorkOrderStatus.CANCELLED))
                .averageCompletionMinutes(averageCompletionMinutes)
                .statusDistribution(toStatusResponses(statusCounts))
                .priorityDistribution(toPriorityResponses(priorityCounts))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SlaReportResponse getSlaReport(String from, String to, String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parse(from, to);
        DashboardServiceImpl.Scope scope = dashboardService.resolveScope(currentUsername);
        SlaMetrics metrics = SlaMetrics.from(
                workOrderRepository.findSlaWorkOrdersInRange(range.from(), range.to(), scope.technicianId()),
                LocalDateTime.now()
        );
        return SlaReportResponse.builder()
                .totalWorkOrdersWithSla(metrics.getWithSla())
                .resolvedWithinSla(metrics.getResolvedWithinSla())
                .breached(metrics.getBreached())
                .atRisk(metrics.getAtRisk())
                .onTrack(metrics.getOnTrack())
                .resolved(metrics.getResolved())
                .slaCompliancePercentage(metrics.compliancePercentage())
                .responseSlaBreaches(metrics.getResponseBreaches())
                .resolutionSlaBreaches(metrics.getResolutionBreaches())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TechnicianPerformanceResponse> getTechnicianPerformance(String from, String to, String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parse(from, to);
        DashboardServiceImpl.Scope scope = dashboardService.resolveScope(currentUsername);
        LocalDateTime now = LocalDateTime.now();

        Map<Long, PerformanceAcc> rows = new LinkedHashMap<>();
        for (TechnicianStatusCountProjection projection : workOrderRepository.countTechnicianStatusInRange(
                range.from(), range.to(), scope.technicianId())) {
            PerformanceAcc acc = rows.computeIfAbsent(
                    projection.getTechnicianId(),
                    id -> new PerformanceAcc(id, DashboardServiceImpl.formatName(projection.getFirstName(), projection.getLastName()))
            );
            acc.assignedCount += projection.getTotal();
            if (projection.getStatus() == WorkOrderStatus.COMPLETED) {
                acc.completedCount += projection.getTotal();
            }
            if (projection.getStatus() == WorkOrderStatus.CLOSED) {
                acc.closedCount += projection.getTotal();
            }
        }

        for (WorkOrder workOrder : workOrderRepository.findSlaWorkOrdersInRange(
                range.from(), range.to(), scope.technicianId())) {
            if (workOrder.getAssignedTechnician() == null) {
                continue;
            }
            PerformanceAcc acc = rows.computeIfAbsent(
                    workOrder.getAssignedTechnician().getId(),
                    id -> new PerformanceAcc(id, DashboardServiceImpl.formatName(
                            workOrder.getAssignedTechnician().getFirstName(),
                            workOrder.getAssignedTechnician().getLastName()))
            );
            SlaStatus status = SlaCalculator.calculateStatus(workOrder, now);
            if (status == SlaStatus.RESOLVED) {
                acc.completedWithinSla++;
            }
            if (status == SlaStatus.BREACHED) {
                acc.slaBreaches++;
            }
        }

        for (TechnicianMinutesProjection projection : timeLogRepository.sumMinutesByTechnicianInRange(
                range.from(), range.to(), scope.technicianId())) {
            PerformanceAcc acc = rows.computeIfAbsent(
                    projection.getTechnicianId(),
                    id -> new PerformanceAcc(id, DashboardServiceImpl.formatName(projection.getFirstName(), projection.getLastName()))
            );
            acc.totalLoggedMinutes = projection.getTotalMinutes();
        }

        List<TechnicianPerformanceResponse> result = new ArrayList<>();
        for (PerformanceAcc acc : rows.values()) {
            result.add(TechnicianPerformanceResponse.builder()
                    .technicianId(acc.technicianId)
                    .technicianName(acc.technicianName)
                    .assignedCount(acc.assignedCount)
                    .completedCount(acc.completedCount)
                    .closedCount(acc.closedCount)
                    .completedWithinSla(acc.completedWithinSla)
                    .slaBreaches(acc.slaBreaches)
                    .totalLoggedMinutes(acc.totalLoggedMinutes)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport(String currentUsername) {
        dashboardService.resolveScope(currentUsername);
        long total = partRepository.count();
        long active = partRepository.countByStatus(PartStatus.ACTIVE);
        BigDecimal inventoryValue = partRepository.sumInventoryValue();
        return InventoryReportResponse.builder()
                .totalParts(total)
                .activeParts(active)
                .inactiveParts(partRepository.countByStatus(PartStatus.INACTIVE))
                .lowStock(partRepository.countLowStock())
                .outOfStock(partRepository.countOutOfStock())
                .totalInventoryValue(inventoryValue == null ? BigDecimal.ZERO : inventoryValue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TimeReportResponse getTimeReport(String from, String to, String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parse(from, to);
        DashboardServiceImpl.Scope scope = dashboardService.resolveScope(currentUsername);
        long minutes = timeLogRepository.sumDurationMinutesInRange(range.from(), range.to(), scope.technicianId());

        return TimeReportResponse.builder()
                .totalLoggedMinutes(minutes)
                .totalLoggedHours(DashboardServiceImpl.toHours(minutes))
                .logsCount(timeLogRepository.countInRange(range.from(), range.to(), scope.technicianId()))
                .minutesByTechnician(timeLogRepository.sumMinutesByTechnicianInRange(
                                range.from(), range.to(), scope.technicianId())
                        .stream()
                        .map(row -> TechnicianTimeTotalResponse.builder()
                                .technicianId(row.getTechnicianId())
                                .technicianName(DashboardServiceImpl.formatName(row.getFirstName(), row.getLastName()))
                                .totalLoggedMinutes(row.getTotalMinutes())
                                .build())
                        .toList())
                .minutesByWorkOrder(timeLogRepository.sumMinutesByWorkOrderInRange(
                                range.from(), range.to(), scope.technicianId())
                        .stream()
                        .map(row -> WorkOrderTimeTotalResponse.builder()
                                .workOrderId(row.getWorkOrderId())
                                .workOrderNumber(row.getWorkOrderNumber())
                                .title(row.getTitle())
                                .totalLoggedMinutes(row.getTotalMinutes())
                                .build())
                        .toList())
                .build();
    }

    private Map<WorkOrderStatus, Long> completeStatusMap(List<StatusCountProjection> rows) {
        Map<WorkOrderStatus, Long> counts = new EnumMap<>(WorkOrderStatus.class);
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (StatusCountProjection row : rows) {
            if (row.getStatus() != null) {
                counts.put(row.getStatus(), row.getTotal());
            }
        }
        return counts;
    }

    private Map<WorkOrderPriority, Long> completePriorityMap(List<PriorityCountProjection> rows) {
        Map<WorkOrderPriority, Long> counts = new EnumMap<>(WorkOrderPriority.class);
        for (WorkOrderPriority priority : WorkOrderPriority.values()) {
            counts.put(priority, 0L);
        }
        for (PriorityCountProjection row : rows) {
            if (row.getPriority() != null) {
                counts.put(row.getPriority(), row.getTotal());
            }
        }
        return counts;
    }

    private List<StatusCountResponse> toStatusResponses(Map<WorkOrderStatus, Long> counts) {
        List<StatusCountResponse> responses = new ArrayList<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            responses.add(StatusCountResponse.builder().status(status).count(counts.get(status)).build());
        }
        return responses;
    }

    private List<PriorityCountResponse> toPriorityResponses(Map<WorkOrderPriority, Long> counts) {
        List<PriorityCountResponse> responses = new ArrayList<>();
        for (WorkOrderPriority priority : WorkOrderPriority.values()) {
            responses.add(PriorityCountResponse.builder().priority(priority).count(counts.get(priority)).build());
        }
        return responses;
    }

    private static final class PerformanceAcc {
        private final Long technicianId;
        private final String technicianName;
        private long assignedCount;
        private long completedCount;
        private long closedCount;
        private long completedWithinSla;
        private long slaBreaches;
        private long totalLoggedMinutes;

        private PerformanceAcc(Long technicianId, String technicianName) {
            this.technicianId = technicianId;
            this.technicianName = technicianName;
        }
    }
}
