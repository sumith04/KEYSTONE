package com.keystone.service;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.dto.WorkOrderSummaryResponse;
import com.keystone.entity.Customer;
import com.keystone.entity.Site;
import com.keystone.entity.User;
import com.keystone.entity.WorkOrder;
import com.keystone.enums.CustomerStatus;
import com.keystone.enums.Role;
import com.keystone.enums.SiteStatus;
import com.keystone.enums.WorkOrderPriority;
import com.keystone.enums.WorkOrderStatus;
import com.keystone.enums.WorkType;
import com.keystone.exception.ApiException;
import com.keystone.exception.ResourceNotFoundException;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import com.keystone.repository.TimeLogRepository;
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderPartRepository;
import com.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderTechnicianScopingTest {

    private static final String TECH_A_EMAIL = "tech.a@keystone.com";
    private static final String TECH_B_EMAIL = "tech.b@keystone.com";
    private static final String ADMIN_EMAIL = "admin@keystone.com";
    private static final String DISPATCHER_EMAIL = "dispatcher@keystone.com";

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkOrderPartRepository workOrderPartRepository;

    @Mock
    private TimeLogRepository timeLogRepository;

    @Mock
    private SlaService slaService;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    private User technicianA;
    private User technicianB;
    private User admin;
    private User dispatcher;
    private WorkOrder assignedToA;
    private WorkOrder assignedToB;

    @BeforeEach
    void setUp() {
        technicianA = user(25L, "Terry", "Alpha", TECH_A_EMAIL, Role.TECHNICIAN);
        technicianB = user(26L, "Pat", "Bravo", TECH_B_EMAIL, Role.TECHNICIAN);
        admin = user(1L, "Avery", "Admin", ADMIN_EMAIL, Role.ADMIN);
        dispatcher = user(2L, "Dana", "Dispatch", DISPATCHER_EMAIL, Role.DISPATCHER);

        Customer customer = Customer.builder()
                .id(1L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .status(CustomerStatus.ACTIVE)
                .build();
        Site site = Site.builder()
                .id(10L)
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customer(customer)
                .status(SiteStatus.ACTIVE)
                .build();

        assignedToA = workOrder(7L, "WO-000007", WorkOrderStatus.ASSIGNED, customer, site, technicianA, dispatcher);
        assignedToB = workOrder(8L, "WO-000008", WorkOrderStatus.ASSIGNED, customer, site, technicianB, dispatcher);

        lenient().when(userRepository.findByUserEmail(TECH_A_EMAIL)).thenReturn(Optional.of(technicianA));
        lenient().when(userRepository.findByUserEmail(TECH_B_EMAIL)).thenReturn(Optional.of(technicianB));
        lenient().when(userRepository.findByUserEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin));
        lenient().when(userRepository.findByUserEmail(DISPATCHER_EMAIL)).thenReturn(Optional.of(dispatcher));
    }

    @Test
    void technicianA_ShouldSeeOnlyOwnAssignedWorkOrders() {
        when(workOrderRepository.searchWorkOrders(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(25L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignedToA)));

        WorkOrderPageResponse response = workOrderService.getWorkOrders(
                0, 10, "createdAt,desc", null, null, null, null, null, 26L, null, TECH_A_EMAIL);

        assertEquals(1, response.getContent().size());
        assertEquals("WO-000007", response.getContent().get(0).getWorkOrderNumber());
        verify(workOrderRepository).searchWorkOrders(
                isNull(), isNull(), isNull(), isNull(), isNull(), eq(25L), any(Pageable.class));
        verify(workOrderRepository, never()).searchWorkOrders(
                any(), any(), any(), any(), any(), eq(26L), any(Pageable.class));
    }

    @Test
    void technicianA_ShouldNotRetrieveTechnicianBWorkOrderById() {
        when(workOrderRepository.findByIdWithRelations(8L)).thenReturn(Optional.of(assignedToB));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> workOrderService.getWorkOrderById(8L, TECH_A_EMAIL));
        assertTrue(exception.getMessage().contains("Work order not found with id: 8"));
    }

    @Test
    void technicianA_ShouldNotStartHoldResumeOrCompleteTechnicianBWorkOrder() {
        when(workOrderRepository.findByIdWithRelations(8L)).thenReturn(Optional.of(assignedToB));

        assertThrows(ResourceNotFoundException.class, () -> workOrderService.startWorkOrder(8L, TECH_A_EMAIL));
        assertThrows(ResourceNotFoundException.class, () -> workOrderService.holdWorkOrder(8L, TECH_A_EMAIL));
        assertThrows(ResourceNotFoundException.class, () -> workOrderService.resumeWorkOrder(8L, TECH_A_EMAIL));
        assertThrows(ResourceNotFoundException.class, () -> workOrderService.completeWorkOrder(8L, TECH_A_EMAIL));
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void technicianA_ShouldExecuteLifecycleOnOwnWorkOrder() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(assignedToA));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrderService.startWorkOrder(7L, TECH_A_EMAIL).getStatus());
        assertNotNull(assignedToA.getActualStart());

        assertEquals(WorkOrderStatus.ON_HOLD, workOrderService.holdWorkOrder(7L, TECH_A_EMAIL).getStatus());
        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrderService.resumeWorkOrder(7L, TECH_A_EMAIL).getStatus());

        WorkOrderResponse completed = workOrderService.completeWorkOrder(7L, TECH_A_EMAIL);
        assertEquals(WorkOrderStatus.COMPLETED, completed.getStatus());
        assertNotNull(assignedToA.getActualEnd());
    }

    @Test
    void technicianA_ShouldNotAssignWorkOrders() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(26L).build(), TECH_A_EMAIL));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void adminAndDispatcher_ShouldContinueToSeeUnscopedWorkOrders() {
        when(workOrderRepository.searchWorkOrders(
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(assignedToA, assignedToB)));

        WorkOrderPageResponse adminPage = workOrderService.getWorkOrders(
                0, 10, "createdAt", null, null, null, null, null, null, null, ADMIN_EMAIL);
        WorkOrderPageResponse dispatcherPage = workOrderService.getWorkOrders(
                0, 10, "createdAt", null, null, null, null, null, null, null, DISPATCHER_EMAIL);

        assertEquals(2, adminPage.getContent().size());
        assertEquals(2, dispatcherPage.getContent().size());
    }

    @Test
    void technicianSummary_ShouldCountOnlyOwnWorkOrders() {
        when(workOrderRepository.countByAssignedTechnicianIdAndStatus(25L, WorkOrderStatus.ASSIGNED)).thenReturn(2L);
        when(workOrderRepository.countByAssignedTechnicianIdAndStatus(25L, WorkOrderStatus.IN_PROGRESS)).thenReturn(1L);
        when(workOrderRepository.countByAssignedTechnicianIdAndStatus(25L, WorkOrderStatus.ON_HOLD)).thenReturn(1L);
        when(workOrderRepository.countByAssignedTechnicianIdAndStatus(25L, WorkOrderStatus.COMPLETED)).thenReturn(3L);

        WorkOrderSummaryResponse summary = workOrderService.getWorkOrderSummary(TECH_A_EMAIL);

        assertEquals(2L, summary.getAssigned());
        assertEquals(1L, summary.getInProgress());
        assertEquals(1L, summary.getOnHold());
        assertEquals(3L, summary.getCompleted());
        verify(workOrderRepository, never()).countByStatus(any());
    }

    @Test
    void invalidTransition_ShouldStillBeRejectedForOwnWorkOrder() {
        assignedToA.setStatus(WorkOrderStatus.ASSIGNED);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(assignedToA));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.completeWorkOrder(7L, TECH_A_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    private User user(Long id, String firstName, String lastName, String email, Role role) {
        return User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .userEmail(email)
                .role(role)
                .enabled(true)
                .build();
    }

    private WorkOrder workOrder(
            Long id,
            String number,
            WorkOrderStatus status,
            Customer customer,
            Site site,
            User technician,
            User createdBy) {
        return WorkOrder.builder()
                .id(id)
                .workOrderNumber(number)
                .title("Service call")
                .customer(customer)
                .site(site)
                .assignedTechnician(technician)
                .status(status)
                .priority(WorkOrderPriority.HIGH)
                .workType(WorkType.CORRECTIVE_MAINTENANCE)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
