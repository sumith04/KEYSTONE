package com.keystone.service;

import com.keystone.dto.DashboardSummaryResponse;
import com.keystone.dto.InventoryDashboardResponse;
import com.keystone.dto.PriorityCountResponse;
import com.keystone.dto.RecentWorkOrderResponse;
import com.keystone.dto.SlaDashboardResponse;
import com.keystone.dto.StatusCountResponse;
import com.keystone.dto.TechnicianWorkloadResponse;
import com.keystone.dto.TimeDashboardResponse;
import com.keystone.dto.WorkOrderTrendResponse;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.PartStatus;
import com.keystone.enums.Role;
import com.keystone.enums.SlaStatus;
import com.keystone.enums.TrendInterval;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.exception.ApiException;
import com.keystone.repository.PartRepository;
import com.keystone.repository.TimeLogRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.repository.projection.PriorityCountProjection;
import com.keystone.repository.projection.StatusCountProjection;
import com.keystone.repository.projection.TechnicianStatusCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    static final int DEFAULT_RECENT_LIMIT = 10;
    static final int MAX_RECENT_LIMIT = 50;
    static final int DEFAULT_TREND_DAYS = 30;

    private final WorkOrderRepository workOrderRepository;
    private final PartRepository partRepository;
    private final TimeLogRepository timeLogRepository;
    private final UserRepository userRepository;
    private final WorkOrderAccessGuard workOrderAccessGuard;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String from, String to, String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parse(from, to);
        Scope scope = resolveScope(currentUsername);
        LocalDateTime now = LocalDateTime.now();

        Map<WorkOrderStatus, Long> statusCounts = toStatusMap(
                workOrderRepository.countByStatusInRange(range.from(), range.to(), scope.technicianId()));
        Map<WorkOrderPriority, Long> priorityCounts = toPriorityMap(
                workOrderRepository.countByPriorityInRange(range.from(), range.to(), scope.technicianId()));
        long total = workOrderRepository.countCreatedInRange(range.from(), range.to(), scope.technicianId());

        SlaMetrics slaMetrics = SlaMetrics.from(
                workOrderRepository.findSlaWorkOrdersInRange(range.from(), range.to(), scope.technicianId()),
                now
        );

        long totalParts = partRepository.count();
        long activeParts = partRepository.countByStatus(PartStatus.ACTIVE);
        long lowStockParts = partRepository.countLowStock();
        long outOfStockParts = partRepository.countOutOfStock();
        long loggedMinutes = timeLogRepository.sumDurationMinutesInRange(range.from(), range.to(), scope.technicianId());
        long techniciansWithLogs = timeLogRepository.countDistinctTechniciansInRange(
                range.from(), range.to(), scope.technicianId());

        long newCount = statusCounts.get(WorkOrderStatus.NEW);
        long assigned = statusCounts.get(WorkOrderStatus.ASSIGNED);
        long inProgress = statusCounts.get(WorkOrderStatus.IN_PROGRESS);
        long onHold = statusCounts.get(WorkOrderStatus.ON_HOLD);

        return DashboardSummaryResponse.builder()
                .totalWorkOrders(total)
                .newWorkOrders(newCount)
                .assignedWorkOrders(assigned)
                .inProgressWorkOrders(inProgress)
                .onHoldWorkOrders(onHold)
                .completedWorkOrders(statusCounts.get(WorkOrderStatus.COMPLETED))
                .closedWorkOrders(statusCounts.get(WorkOrderStatus.CLOSED))
                .cancelledWorkOrders(statusCounts.get(WorkOrderStatus.CANCELLED))
                .openWorkOrders(newCount + assigned + inProgress + onHold)
                .slaBreachedWorkOrders(slaMetrics.getBreached())
                .slaAtRiskWorkOrders(slaMetrics.getAtRisk())
                .activeTechnicians(scope.technicianId() != null ? 1 : userRepository.countByRoleAndEnabled(Role.TECHNICIAN, true))
                .lowStockParts(lowStockParts)
                .totalParts(totalParts)
                .statusDistribution(completeStatusDistribution(statusCounts))
                .priorityDistribution(completePriorityDistribution(priorityCounts))
                .technicianWorkload(buildWorkload(range, scope, now))
                .sla(SlaDashboardResponse.builder()
                        .onTrack(slaMetrics.getOnTrack())
                        .atRisk(slaMetrics.getAtRisk())
                        .breached(slaMetrics.getBreached())
                        .resolved(slaMetrics.getResolved())
                        .slaCompliancePercentage(slaMetrics.compliancePercentage())
                        .build())
                .inventory(InventoryDashboardResponse.builder()
                        .totalParts(totalParts)
                        .activeParts(activeParts)
                        .lowStockParts(lowStockParts)
                        .outOfStockParts(outOfStockParts)
                        .build())
                .time(TimeDashboardResponse.builder()
                        .totalLoggedMinutes(loggedMinutes)
                        .totalLoggedHours(toHours(loggedMinutes))
                        .activeTechniciansWithTimeLogs(techniciansWithLogs)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentWorkOrderResponse> getRecentWorkOrders(
            String from,
            String to,
            Integer limit,
            String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parse(from, to);
        Scope scope = resolveScope(currentUsername);
        int size = normalizeLimit(limit);
        LocalDateTime now = LocalDateTime.now();
        return workOrderRepository.findRecentCreatedInRange(
                        range.from(), range.to(), scope.technicianId(), PageRequest.of(0, size))
                .stream()
                .map(workOrder -> RecentWorkOrderResponse.fromEntity(workOrder, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderTrendResponse> getWorkOrderTrend(
            String from,
            String to,
            String interval,
            String currentUsername) {
        ReportingDateRange range = ReportingDateRange.parseOrLastDays(from, to, DEFAULT_TREND_DAYS);
        Scope scope = resolveScope(currentUsername);
        TrendInterval trendInterval = parseInterval(interval);

        LocalDate start = range.fromDate() != null ? range.fromDate() : LocalDate.now().minusDays(DEFAULT_TREND_DAYS - 1L);
        LocalDate end = range.toDate() != null ? range.toDate() : LocalDate.now();

        Map<String, WorkOrderTrendResponse> buckets = emptyBuckets(start, end, trendInterval);
        increment(buckets, workOrderRepository.findCreatedAtInRange(range.from(), range.to(), scope.technicianId()), trendInterval, "created");
        increment(buckets, workOrderRepository.findActualEndInRange(range.from(), range.to(), scope.technicianId()), trendInterval, "completed");
        increment(buckets, workOrderRepository.findClosedUpdatedAtInRange(range.from(), range.to(), scope.technicianId()), trendInterval, "closed");
        return new ArrayList<>(buckets.values());
    }

    Scope resolveScope(String currentUsername) {
        User currentUser = workOrderAccessGuard.requireCurrentUser(currentUsername);
        if (workOrderAccessGuard.isTechnician(currentUser)) {
            return new Scope(currentUser.getId());
        }
        return new Scope(null);
    }

    private List<TechnicianWorkloadResponse> buildWorkload(ReportingDateRange range, Scope scope, LocalDateTime now) {
        Map<Long, WorkloadAcc> rows = new LinkedHashMap<>();

        for (TechnicianStatusCountProjection row : workOrderRepository.countTechnicianStatusInRange(
                range.from(), range.to(), scope.technicianId())) {
            WorkloadAcc acc = rows.computeIfAbsent(
                    row.getTechnicianId(),
                    id -> new WorkloadAcc(id, formatName(row.getFirstName(), row.getLastName()))
            );
            if (row.getStatus() == WorkOrderStatus.ASSIGNED) {
                acc.assignedWorkOrders += row.getTotal();
            } else if (row.getStatus() == WorkOrderStatus.IN_PROGRESS) {
                acc.inProgressWorkOrders += row.getTotal();
            } else if (row.getStatus() == WorkOrderStatus.COMPLETED || row.getStatus() == WorkOrderStatus.CLOSED) {
                acc.completedWorkOrders += row.getTotal();
            }
        }

        for (WorkOrder workOrder : workOrderRepository.findSlaWorkOrdersInRange(
                range.from(), range.to(), scope.technicianId())) {
            if (workOrder.getAssignedTechnician() == null) {
                continue;
            }
            if (SlaCalculator.calculateStatus(workOrder, now) == SlaStatus.BREACHED) {
                WorkloadAcc acc = rows.computeIfAbsent(
                        workOrder.getAssignedTechnician().getId(),
                        id -> new WorkloadAcc(
                                id,
                                formatName(
                                        workOrder.getAssignedTechnician().getFirstName(),
                                        workOrder.getAssignedTechnician().getLastName())
                        )
                );
                acc.overdueSlaWorkOrders++;
            }
        }

        List<TechnicianWorkloadResponse> result = new ArrayList<>();
        for (WorkloadAcc acc : rows.values()) {
            result.add(TechnicianWorkloadResponse.builder()
                    .technicianId(acc.technicianId)
                    .technicianName(acc.technicianName)
                    .assignedWorkOrders(acc.assignedWorkOrders)
                    .inProgressWorkOrders(acc.inProgressWorkOrders)
                    .completedWorkOrders(acc.completedWorkOrders)
                    .overdueSlaWorkOrders(acc.overdueSlaWorkOrders)
                    .build());
        }
        return result;
    }

    private Map<WorkOrderStatus, Long> toStatusMap(List<StatusCountProjection> rows) {
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

    private Map<WorkOrderPriority, Long> toPriorityMap(List<PriorityCountProjection> rows) {
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

    private List<StatusCountResponse> completeStatusDistribution(Map<WorkOrderStatus, Long> counts) {
        List<StatusCountResponse> distribution = new ArrayList<>();
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            distribution.add(StatusCountResponse.builder().status(status).count(counts.get(status)).build());
        }
        return distribution;
    }

    private List<PriorityCountResponse> completePriorityDistribution(Map<WorkOrderPriority, Long> counts) {
        List<PriorityCountResponse> distribution = new ArrayList<>();
        for (WorkOrderPriority priority : WorkOrderPriority.values()) {
            distribution.add(PriorityCountResponse.builder().priority(priority).count(counts.get(priority)).build());
        }
        return distribution;
    }

    private Map<String, WorkOrderTrendResponse> emptyBuckets(LocalDate start, LocalDate end, TrendInterval interval) {
        Map<String, WorkOrderTrendResponse> buckets = new LinkedHashMap<>();
        LocalDate cursor = periodStart(start, interval);
        LocalDate last = periodStart(end, interval);
        while (!cursor.isAfter(last)) {
            String key = periodKey(cursor, interval);
            buckets.put(key, WorkOrderTrendResponse.builder().period(key).created(0).completed(0).closed(0).build());
            cursor = nextPeriod(cursor, interval);
        }
        return buckets;
    }

    private void increment(
            Map<String, WorkOrderTrendResponse> buckets,
            List<LocalDateTime> timestamps,
            TrendInterval interval,
            String field) {
        for (LocalDateTime timestamp : timestamps) {
            if (timestamp == null) {
                continue;
            }
            String key = periodKey(timestamp.toLocalDate(), interval);
            WorkOrderTrendResponse current = buckets.get(key);
            if (current == null) {
                continue;
            }
            if ("created".equals(field)) {
                current.setCreated(current.getCreated() + 1);
            } else if ("completed".equals(field)) {
                current.setCompleted(current.getCompleted() + 1);
            } else {
                current.setClosed(current.getClosed() + 1);
            }
        }
    }

    private LocalDate periodStart(LocalDate date, TrendInterval interval) {
        return switch (interval) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate nextPeriod(LocalDate date, TrendInterval interval) {
        return switch (interval) {
            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }

    private String periodKey(LocalDate date, TrendInterval interval) {
        return periodStart(date, interval).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private TrendInterval parseInterval(String interval) {
        if (interval == null || interval.isBlank()) {
            return TrendInterval.DAY;
        }
        try {
            return TrendInterval.valueOf(interval.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException("interval must be DAY, WEEK, or MONTH.", HttpStatus.BAD_REQUEST);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_RECENT_LIMIT;
        }
        if (limit < 1) {
            throw new ApiException("limit must be at least 1.", HttpStatus.BAD_REQUEST);
        }
        return Math.min(limit, MAX_RECENT_LIMIT);
    }

    static BigDecimal toHours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    static String formatName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        return (first + " " + last).trim();
    }

    record Scope(Long technicianId) {
    }

    private static final class WorkloadAcc {
        private final Long technicianId;
        private final String technicianName;
        private long assignedWorkOrders;
        private long inProgressWorkOrders;
        private long completedWorkOrders;
        private long overdueSlaWorkOrders;

        private WorkloadAcc(Long technicianId, String technicianName) {
            this.technicianId = technicianId;
            this.technicianName = technicianName;
        }
    }
}
