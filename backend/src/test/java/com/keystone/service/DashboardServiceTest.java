package com.keystone.service;

import com.keystone.dto.DashboardSummaryResponse;
import com.keystone.dto.RecentWorkOrderResponse;
import com.keystone.dto.WorkOrderTrendResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.PartStatus;
import com.keystone.enums.Role;
import com.keystone.enums.SlaStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String MANAGER_EMAIL = "manager@keystone.com";
    private static final String TECH_EMAIL = "tech.a@keystone.com";

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private PartRepository partRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkOrderAccessGuard workOrderAccessGuard;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User manager;
    private User technician;
    private WorkOrder slaWorkOrder;

    @BeforeEach
    void setUp() {
        manager = User.builder().id(10L).userEmail(MANAGER_EMAIL).role(Role.MANAGER).firstName("Mo").lastName("Manager").enabled(true).build();
        technician = User.builder().id(25L).userEmail(TECH_EMAIL).role(Role.TECHNICIAN).firstName("Ty").lastName("Tech").enabled(true).build();
        LocalDateTime created = LocalDateTime.of(2026, 1, 10, 8, 0);
        slaWorkOrder = WorkOrder.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .title("HVAC repair")
                .status(WorkOrderStatus.ASSIGNED)
                .priority(WorkOrderPriority.HIGH)
                .customer(Customer.builder().companyName("Acme").build())
                .site(Site.builder().siteName("HQ").build())
                .assignedTechnician(technician)
                .createdAt(created)
                .slaPolicyName("Gold")
                .slaResponseDueAt(created.plusHours(2))
                .slaResolutionDueAt(created.plusHours(8))
                .build();
    }

    @Test
    void getSummary_WhenManager_ShouldAggregateOrganizationMetrics() {
        when(workOrderAccessGuard.requireCurrentUser(MANAGER_EMAIL)).thenReturn(manager);
        when(workOrderAccessGuard.isTechnician(manager)).thenReturn(false);
        stubSummaryQueries(null);
        when(workOrderRepository.findSlaWorkOrdersInRange(isNull(), isNull(), isNull()))
                .thenReturn(List.of(slaWorkOrder));
        when(userRepository.countByRoleAndEnabled(Role.TECHNICIAN, true)).thenReturn(14L);

        DashboardSummaryResponse response = dashboardService.getSummary(null, null, MANAGER_EMAIL);

        assertEquals(120, response.getTotalWorkOrders());
        assertEquals(12, response.getNewWorkOrders());
        assertEquals(18, response.getAssignedWorkOrders());
        assertEquals(25, response.getInProgressWorkOrders());
        assertEquals(8, response.getOnHoldWorkOrders());
        assertEquals(40, response.getCompletedWorkOrders());
        assertEquals(15, response.getClosedWorkOrders());
        assertEquals(2, response.getCancelledWorkOrders());
        assertEquals(63, response.getOpenWorkOrders());
        assertEquals(7, response.getStatusDistribution().size());
        assertEquals(4, response.getPriorityDistribution().size());
        assertEquals(14, response.getActiveTechnicians());
        assertEquals(80, response.getTotalParts());
        assertEquals(5, response.getLowStockParts());
        assertEquals(3, response.getInventory().getOutOfStockParts());
        assertEquals(12450, response.getTime().getTotalLoggedMinutes());
        assertEquals(new BigDecimal("207.5"), response.getTime().getTotalLoggedHours());
        assertEquals(1, response.getTechnicianWorkload().size());
        assertEquals(10, response.getTechnicianWorkload().get(0).getAssignedWorkOrders());
        assertEquals(SlaStatus.ON_TRACK, SlaCalculator.calculateStatus(slaWorkOrder, LocalDateTime.of(2026, 1, 10, 8, 30)));
        verify(workOrderRepository).countCreatedInRange(isNull(), isNull(), isNull());
    }

    @Test
    void getSummary_WhenTechnician_ShouldScopeQueries() {
        when(workOrderAccessGuard.requireCurrentUser(TECH_EMAIL)).thenReturn(technician);
        when(workOrderAccessGuard.isTechnician(technician)).thenReturn(true);
        stubSummaryQueries(25L);
        when(workOrderRepository.findSlaWorkOrdersInRange(isNull(), isNull(), eq(25L))).thenReturn(List.of());

        DashboardSummaryResponse response = dashboardService.getSummary(null, null, TECH_EMAIL);

        assertEquals(1, response.getActiveTechnicians());
        verify(workOrderRepository).countCreatedInRange(isNull(), isNull(), eq(25L));
        verify(timeLogRepository).sumDurationMinutesInRange(isNull(), isNull(), eq(25L));
    }

    @Test
    void getSummary_WhenInvalidDateRange_ShouldReturn400() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> dashboardService.getSummary("2026-02-01", "2026-01-01", MANAGER_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getRecentWorkOrders_ShouldCapLimitAndMapSlaStatus() {
        when(workOrderAccessGuard.requireCurrentUser(MANAGER_EMAIL)).thenReturn(manager);
        when(workOrderAccessGuard.isTechnician(manager)).thenReturn(false);
        when(workOrderRepository.findRecentCreatedInRange(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(List.of(slaWorkOrder));

        List<RecentWorkOrderResponse> recent = dashboardService.getRecentWorkOrders(null, null, 200, MANAGER_EMAIL);

        assertEquals(1, recent.size());
        assertEquals("WO-000007", recent.get(0).getWorkOrderNumber());
        assertEquals("Acme", recent.get(0).getCustomer());
        assertEquals("HQ", recent.get(0).getSite());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(workOrderRepository).findRecentCreatedInRange(isNull(), isNull(), isNull(), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void getWorkOrderTrend_WhenInvalidInterval_ShouldReturn400() {
        when(workOrderAccessGuard.requireCurrentUser(MANAGER_EMAIL)).thenReturn(manager);
        when(workOrderAccessGuard.isTechnician(manager)).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> dashboardService.getWorkOrderTrend("2026-01-01", "2026-01-03", "YEAR", MANAGER_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getWorkOrderTrend_ShouldBucketWithoutPerDayQueries() {
        when(workOrderAccessGuard.requireCurrentUser(MANAGER_EMAIL)).thenReturn(manager);
        when(workOrderAccessGuard.isTechnician(manager)).thenReturn(false);
        when(workOrderRepository.findCreatedAtInRange(any(), any(), isNull()))
                .thenReturn(List.of(LocalDateTime.of(2026, 1, 1, 9, 0), LocalDateTime.of(2026, 1, 1, 11, 0)));
        when(workOrderRepository.findActualEndInRange(any(), any(), isNull()))
                .thenReturn(List.of(LocalDateTime.of(2026, 1, 2, 16, 0)));
        when(workOrderRepository.findClosedUpdatedAtInRange(any(), any(), isNull()))
                .thenReturn(List.of(LocalDateTime.of(2026, 1, 3, 12, 0)));

        List<WorkOrderTrendResponse> trend = dashboardService.getWorkOrderTrend("2026-01-01", "2026-01-03", "DAY", MANAGER_EMAIL);

        assertEquals(3, trend.size());
        assertEquals("2026-01-01", trend.get(0).getPeriod());
        assertEquals(2, trend.get(0).getCreated());
        assertEquals(1, trend.get(1).getCompleted());
        assertEquals(1, trend.get(2).getClosed());
    }

    @Test
    void slaCompliance_WhenNoResolvedSla_ShouldBeNull() {
        when(workOrderAccessGuard.requireCurrentUser(MANAGER_EMAIL)).thenReturn(manager);
        when(workOrderAccessGuard.isTechnician(manager)).thenReturn(false);
        stubSummaryQueries(null);
        when(workOrderRepository.findSlaWorkOrdersInRange(isNull(), isNull(), isNull())).thenReturn(List.of(slaWorkOrder));
        when(userRepository.countByRoleAndEnabled(Role.TECHNICIAN, true)).thenReturn(1L);

        DashboardSummaryResponse response = dashboardService.getSummary(null, null, MANAGER_EMAIL);

        assertNull(response.getSla().getSlaCompliancePercentage());
        assertTrue(response.getSla().getOnTrack() + response.getSla().getAtRisk() + response.getSla().getBreached() >= 1);
    }

    private void stubSummaryQueries(Long technicianId) {
        when(workOrderRepository.countCreatedInRange(isNull(), isNull(), eq(technicianId))).thenReturn(120L);
        when(workOrderRepository.countByStatusInRange(isNull(), isNull(), eq(technicianId))).thenReturn(List.of(
                status(WorkOrderStatus.NEW, 12),
                status(WorkOrderStatus.ASSIGNED, 18),
                status(WorkOrderStatus.IN_PROGRESS, 25),
                status(WorkOrderStatus.ON_HOLD, 8),
                status(WorkOrderStatus.COMPLETED, 40),
                status(WorkOrderStatus.CLOSED, 15),
                status(WorkOrderStatus.CANCELLED, 2)
        ));
        when(workOrderRepository.countByPriorityInRange(isNull(), isNull(), eq(technicianId))).thenReturn(List.of(
                priority(WorkOrderPriority.LOW, 10),
                priority(WorkOrderPriority.MEDIUM, 50),
                priority(WorkOrderPriority.HIGH, 45),
                priority(WorkOrderPriority.URGENT, 15)
        ));
        when(workOrderRepository.countTechnicianStatusInRange(isNull(), isNull(), eq(technicianId))).thenReturn(List.of(
                techStatus(25L, "Ty", "Tech", WorkOrderStatus.ASSIGNED, 10),
                techStatus(25L, "Ty", "Tech", WorkOrderStatus.IN_PROGRESS, 4),
                techStatus(25L, "Ty", "Tech", WorkOrderStatus.COMPLETED, 20)
        ));
        when(partRepository.count()).thenReturn(80L);
        when(partRepository.countByStatus(PartStatus.ACTIVE)).thenReturn(70L);
        when(partRepository.countLowStock()).thenReturn(5L);
        when(partRepository.countOutOfStock()).thenReturn(3L);
        when(timeLogRepository.sumDurationMinutesInRange(isNull(), isNull(), eq(technicianId))).thenReturn(12450L);
        when(timeLogRepository.countDistinctTechniciansInRange(isNull(), isNull(), eq(technicianId))).thenReturn(6L);
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
}
