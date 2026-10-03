package com.keystone.service;

import com.keystone.dto.InventoryReportResponse;
import com.keystone.dto.SlaReportResponse;
import com.keystone.dto.TechnicianPerformanceResponse;
import com.keystone.dto.TimeReportResponse;
import com.keystone.dto.WorkOrderReportResponse;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.PartStatus;
import com.keystone.enums.Role;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.repository.PartRepository;
import com.keystone.repository.TimeLogRepository;
import com.keystone.repository.WorkOrderRepository;
import com.keystone.repository.projection.PriorityCountProjection;
import com.keystone.repository.projection.StatusCountProjection;
import com.keystone.repository.projection.TechnicianMinutesProjection;
import com.keystone.repository.projection.TechnicianStatusCountProjection;
import com.keystone.repository.projection.WorkOrderMinutesProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final String MANAGER_EMAIL = "manager@keystone.com";
    private static final String TECH_EMAIL = "tech.a@keystone.com";

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private DashboardServiceImpl dashboardService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private DashboardServiceImpl.Scope orgScope;
    private DashboardServiceImpl.Scope techScope;
    private User technician;
    private WorkOrder resolvedSla;
    private WorkOrder breachedSla;

    @BeforeEach
    void setUp() {
        orgScope = new DashboardServiceImpl.Scope(null);
        techScope = new DashboardServiceImpl.Scope(25L);
        technician = User.builder().id(25L).firstName("Ty").lastName("Tech").role(Role.TECHNICIAN).userEmail(TECH_EMAIL).build();
        LocalDateTime created = LocalDateTime.of(2026, 1, 10, 8, 0);
        resolvedSla = WorkOrder.builder()
                .assignedTechnician(technician)
                .createdAt(created)
                .slaResponseDueAt(created.plusHours(2))
                .slaResolutionDueAt(created.plusHours(8))
                .responseAt(created.plusMinutes(30))
                .responseBreached(false)
                .resolvedAt(created.plusHours(3))
                .resolutionBreached(false)
                .build();
        breachedSla = WorkOrder.builder()
                .assignedTechnician(technician)
                .createdAt(created)
                .slaResponseDueAt(created.plusHours(1))
                .slaResolutionDueAt(created.plusHours(2))
                .responseAt(created.plusHours(3))
                .responseBreached(true)
                .resolvedAt(created.plusHours(4))
                .resolutionBreached(true)
                .build();
    }

    @Test
    void getWorkOrderReport_ShouldReturnDistributionsAndAverageCompletion() {
        when(dashboardService.resolveScope(MANAGER_EMAIL)).thenReturn(orgScope);
        when(workOrderRepository.countCreatedInRange(isNull(), isNull(), isNull())).thenReturn(10L);
        when(workOrderRepository.countByStatusInRange(isNull(), isNull(), isNull())).thenReturn(List.of(
                status(WorkOrderStatus.COMPLETED, 4),
                status(WorkOrderStatus.CLOSED, 2),
                status(WorkOrderStatus.CANCELLED, 1)
        ));
        when(workOrderRepository.countByPriorityInRange(isNull(), isNull(), isNull())).thenReturn(List.of(
                priority(WorkOrderPriority.HIGH, 6)
        ));
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 8, 0);
        when(workOrderRepository.findCompletionPairsInRange(isNull(), isNull(), isNull())).thenReturn(List.of(
                new Object[]{created, created.plusMinutes(60)},
                new Object[]{created, created.plusMinutes(120)}
        ));

        WorkOrderReportResponse response = reportService.getWorkOrderReport(null, null, MANAGER_EMAIL);

        assertEquals(10, response.getTotal());
        assertEquals(10, response.getCreated());
        assertEquals(4, response.getCompleted());
        assertEquals(2, response.getClosed());
        assertEquals(1, response.getCancelled());
        assertEquals(90, response.getAverageCompletionMinutes());
        assertEquals(7, response.getStatusDistribution().size());
        assertEquals(4, response.getPriorityDistribution().size());
    }

    @Test
    void getSlaReport_ShouldReuseCalculatorAndComputeCompliance() {
        when(dashboardService.resolveScope(MANAGER_EMAIL)).thenReturn(orgScope);
        when(workOrderRepository.findSlaWorkOrdersInRange(isNull(), isNull(), isNull()))
                .thenReturn(List.of(resolvedSla, breachedSla));

        SlaReportResponse response = reportService.getSlaReport(null, null, MANAGER_EMAIL);

        assertEquals(2, response.getTotalWorkOrdersWithSla());
        assertEquals(1, response.getResolvedWithinSla());
        assertEquals(1, response.getBreached());
        assertEquals(new BigDecimal("50.0"), response.getSlaCompliancePercentage());
        assertEquals(1, response.getResponseSlaBreaches());
        assertEquals(1, response.getResolutionSlaBreaches());
    }

    @Test
    void getSlaReport_WhenNoResolvedSla_ShouldReturnNullCompliance() {
        when(dashboardService.resolveScope(MANAGER_EMAIL)).thenReturn(orgScope);
        when(workOrderRepository.findSlaWorkOrdersInRange(isNull(), isNull(), isNull()))
                .thenReturn(List.of(WorkOrder.builder()
                        .createdAt(LocalDateTime.of(2026, 1, 10, 8, 0))
                        .slaResponseDueAt(LocalDateTime.of(2026, 1, 10, 10, 0))
                        .slaResolutionDueAt(LocalDateTime.of(2026, 1, 10, 16, 0))
                        .build()));

        assertNull(reportService.getSlaReport(null, null, MANAGER_EMAIL).getSlaCompliancePercentage());
    }

    @Test
    void getTechnicianPerformance_WhenTechnician_ShouldStayScoped() {
        when(dashboardService.resolveScope(TECH_EMAIL)).thenReturn(techScope);
        when(workOrderRepository.countTechnicianStatusInRange(isNull(), isNull(), eq(25L))).thenReturn(List.of(
                techStatus(25L, "Ty", "Tech", WorkOrderStatus.ASSIGNED, 3),
                techStatus(25L, "Ty", "Tech", WorkOrderStatus.COMPLETED, 2),
                techStatus(25L, "Ty", "Tech", WorkOrderStatus.CLOSED, 1)
        ));
        when(workOrderRepository.findSlaWorkOrdersInRange(isNull(), isNull(), eq(25L)))
                .thenReturn(List.of(resolvedSla, breachedSla));
        when(timeLogRepository.sumMinutesByTechnicianInRange(isNull(), isNull(), eq(25L))).thenReturn(List.of(
                minutes(25L, "Ty", "Tech", 180)
        ));

        List<TechnicianPerformanceResponse> response = reportService.getTechnicianPerformance(null, null, TECH_EMAIL);

        assertEquals(1, response.size());
        assertEquals(25L, response.get(0).getTechnicianId());
        assertEquals(6, response.get(0).getAssignedCount());
        assertEquals(2, response.get(0).getCompletedCount());
        assertEquals(1, response.get(0).getClosedCount());
        assertEquals(1, response.get(0).getCompletedWithinSla());
        assertEquals(1, response.get(0).getSlaBreaches());
        assertEquals(180, response.get(0).getTotalLoggedMinutes());
        verify(workOrderRepository).countTechnicianStatusInRange(isNull(), isNull(), eq(25L));
    }

    @Test
    void getInventoryReport_ShouldUseAggregates() {
        when(dashboardService.resolveScope(MANAGER_EMAIL)).thenReturn(orgScope);
        when(partRepository.count()).thenReturn(80L);
        when(partRepository.countByStatus(PartStatus.ACTIVE)).thenReturn(70L);
        when(partRepository.countByStatus(PartStatus.INACTIVE)).thenReturn(10L);
        when(partRepository.countLowStock()).thenReturn(5L);
        when(partRepository.countOutOfStock()).thenReturn(3L);
        when(partRepository.sumInventoryValue()).thenReturn(new BigDecimal("1234.50"));

        InventoryReportResponse response = reportService.getInventoryReport(MANAGER_EMAIL);

        assertEquals(80, response.getTotalParts());
        assertEquals(70, response.getActiveParts());
        assertEquals(10, response.getInactiveParts());
        assertEquals(5, response.getLowStock());
        assertEquals(3, response.getOutOfStock());
        assertEquals(new BigDecimal("1234.50"), response.getTotalInventoryValue());
    }

    @Test
    void getTimeReport_ShouldKeepMinutesAuthoritative() {
        when(dashboardService.resolveScope(MANAGER_EMAIL)).thenReturn(orgScope);
        when(timeLogRepository.sumDurationMinutesInRange(isNull(), isNull(), isNull())).thenReturn(90L);
        when(timeLogRepository.countInRange(isNull(), isNull(), isNull())).thenReturn(3L);
        when(timeLogRepository.sumMinutesByTechnicianInRange(isNull(), isNull(), isNull())).thenReturn(List.of(
                minutes(25L, "Ty", "Tech", 90)
        ));
        when(timeLogRepository.sumMinutesByWorkOrderInRange(isNull(), isNull(), isNull())).thenReturn(List.of(
                new WorkOrderMinutesProjection() {
                    @Override
                    public Long getWorkOrderId() {
                        return 7L;
                    }

                    @Override
                    public String getWorkOrderNumber() {
                        return "WO-000007";
                    }

                    @Override
                    public String getTitle() {
                        return "HVAC";
                    }

                    @Override
                    public long getTotalMinutes() {
                        return 90;
                    }
                }
        ));

        TimeReportResponse response = reportService.getTimeReport(null, null, MANAGER_EMAIL);

        assertEquals(90, response.getTotalLoggedMinutes());
        assertEquals(new BigDecimal("1.5"), response.getTotalLoggedHours());
        assertEquals(3, response.getLogsCount());
        assertEquals(1, response.getMinutesByTechnician().size());
        assertEquals(1, response.getMinutesByWorkOrder().size());
    }

    private StatusCountProjection status(WorkOrderStatus status, long total) {
        return new StatusCountProjection() {
            @Override
            public WorkOrderStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private PriorityCountProjection priority(WorkOrderPriority priority, long total) {
        return new PriorityCountProjection() {
            @Override
            public WorkOrderPriority getPriority() {
                return priority;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private TechnicianStatusCountProjection techStatus(
            Long id, String first, String last, WorkOrderStatus status, long total) {
        return new TechnicianStatusCountProjection() {
            @Override
            public Long getTechnicianId() {
                return id;
            }

            @Override
            public String getFirstName() {
                return first;
            }

            @Override
            public String getLastName() {
                return last;
            }

            @Override
            public WorkOrderStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private TechnicianMinutesProjection minutes(Long id, String first, String last, long total) {
        return new TechnicianMinutesProjection() {
            @Override
            public Long getTechnicianId() {
                return id;
            }

            @Override
            public String getFirstName() {
                return first;
            }

            @Override
            public String getLastName() {
                return last;
            }

            @Override
            public long getTotalMinutes() {
                return total;
            }
        };
    }
}
