package com.keystone.service;

import com.keystone.dto.AssignWorkOrderRequest;
import com.keystone.dto.CreateWorkOrderRequest;
import com.keystone.dto.UpdateWorkOrderRequest;
import com.keystone.dto.WorkOrderPageResponse;
import com.keystone.dto.WorkOrderResponse;
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
import com.keystone.repository.UserRepository;
import com.keystone.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkOrderServiceImpl workOrderService;

    private Customer customer;
    private Customer otherCustomer;
    private Site site;
    private Site otherSite;
    private User creator;
    private User technician;
    private User manager;
    private User disabledTechnician;
    private WorkOrder workOrder;

    private static final String DISPATCHER_EMAIL = "dispatcher@keystone.com";

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .customerCode("ACME001")
                .companyName("Acme Facilities")
                .status(CustomerStatus.ACTIVE)
                .build();

        otherCustomer = Customer.builder()
                .id(2L)
                .customerCode("NORTH01")
                .companyName("North Campus")
                .status(CustomerStatus.ACTIVE)
                .build();

        site = Site.builder()
                .id(10L)
                .siteCode("HQ-01")
                .siteName("Headquarters")
                .customer(customer)
                .status(SiteStatus.ACTIVE)
                .build();

        otherSite = Site.builder()
                .id(11L)
                .siteCode("NC-01")
                .siteName("North Building")
                .customer(otherCustomer)
                .status(SiteStatus.ACTIVE)
                .build();

        creator = User.builder()
                .id(100L)
                .firstName("Dana")
                .lastName("Dispatcher")
                .userEmail("dispatcher@keystone.com")
                .role(Role.DISPATCHER)
                .enabled(true)
                .build();

        technician = User.builder()
                .id(200L)
                .firstName("Terry")
                .lastName("Tech")
                .userEmail("terry@keystone.com")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        manager = User.builder()
                .id(201L)
                .firstName("Morgan")
                .lastName("Manager")
                .userEmail("morgan@keystone.com")
                .role(Role.MANAGER)
                .enabled(true)
                .build();

        disabledTechnician = User.builder()
                .id(202L)
                .firstName("Inactive")
                .lastName("Tech")
                .userEmail("inactive@keystone.com")
                .role(Role.TECHNICIAN)
                .enabled(false)
                .build();

        workOrder = baseWorkOrder(WorkOrderStatus.NEW);

        lenient().when(userRepository.findByUserEmail(DISPATCHER_EMAIL)).thenReturn(Optional.of(creator));
    }

    @Test
    void createWorkOrder_WithValidData_ShouldGenerateNumberAndPersist() {
        CreateWorkOrderRequest request = validCreateRequest();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(site));
        when(userRepository.findByUserEmail("dispatcher@keystone.com")).thenReturn(Optional.of(creator));
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder saved = invocation.getArgument(0);
            saved.setId(42L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workOrderRepository.findByIdWithRelations(42L)).thenAnswer(invocation -> {
            workOrder.setId(42L);
            workOrder.setWorkOrderNumber("WO-000042");
            return Optional.of(workOrder);
        });

        WorkOrderResponse response = workOrderService.createWorkOrder(request, "Dispatcher@Keystone.com");

        ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
        verify(workOrderRepository).saveAndFlush(captor.capture());
        assertEquals(WorkOrderStatus.NEW, captor.getValue().getStatus());
        assertEquals("Replace HVAC filter", captor.getValue().getTitle());
        assertEquals(customer, captor.getValue().getCustomer());
        assertEquals(site, captor.getValue().getSite());
        assertEquals(creator, captor.getValue().getCreatedBy());
        assertEquals("WO-000042", response.getWorkOrderNumber());
    }

    @Test
    void createWorkOrder_WhenSiteBelongsToAnotherCustomer_ShouldThrowBadRequest() {
        CreateWorkOrderRequest request = validCreateRequest();
        request.setSiteId(11L);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.findByIdWithCustomer(11L)).thenReturn(Optional.of(otherSite));

        ApiException exception = assertThrows(ApiException.class,
                () -> workOrderService.createWorkOrder(request, "dispatcher@keystone.com"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("does not belong"));
        verify(workOrderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createWorkOrder_WhenCustomerMissing_ShouldThrowNotFound() {
        CreateWorkOrderRequest request = validCreateRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workOrderService.createWorkOrder(request, "dispatcher@keystone.com"));
    }

    @Test
    void createWorkOrder_WhenSiteMissing_ShouldThrowNotFound() {
        CreateWorkOrderRequest request = validCreateRequest();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.findByIdWithCustomer(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workOrderService.createWorkOrder(request, "dispatcher@keystone.com"));
    }

    @Test
    void createWorkOrder_WhenScheduleInvalid_ShouldThrowBadRequest() {
        CreateWorkOrderRequest request = validCreateRequest();
        request.setScheduledStart(LocalDateTime.of(2026, 10, 4, 16, 0));
        request.setScheduledEnd(LocalDateTime.of(2026, 10, 4, 9, 0));

        ApiException exception = assertThrows(ApiException.class,
                () -> workOrderService.createWorkOrder(request, "dispatcher@keystone.com"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void getWorkOrderById_WhenExists_ShouldReturnWorkOrder() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        WorkOrderResponse response = workOrderService.getWorkOrderById(7L, DISPATCHER_EMAIL);

        assertEquals(7L, response.getId());
        assertEquals("WO-000007", response.getWorkOrderNumber());
        assertEquals("Acme Facilities", response.getCustomerName());
        assertEquals("Headquarters", response.getSiteName());
    }

    @Test
    void getWorkOrderById_WhenMissing_ShouldThrowNotFound() {
        when(workOrderRepository.findByIdWithRelations(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> workOrderService.getWorkOrderById(99L, DISPATCHER_EMAIL));
    }

    @Test
    void getWorkOrderByNumber_WhenExists_ShouldReturnWorkOrder() {
        when(workOrderRepository.findByWorkOrderNumberWithRelations("WO-000007")).thenReturn(Optional.of(workOrder));

        WorkOrderResponse response = workOrderService.getWorkOrderByNumber("wo-000007", DISPATCHER_EMAIL);

        assertEquals("WO-000007", response.getWorkOrderNumber());
    }

    @Test
    void getWorkOrders_ShouldReturnPagedSearchResults() {
        when(workOrderRepository.searchWorkOrders(
                eq("hvac"), eq(WorkOrderStatus.NEW), eq(WorkOrderPriority.HIGH), eq(1L), eq(10L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(workOrder)));

        WorkOrderPageResponse response = workOrderService.getWorkOrders(
                0, 10, "createdAt,desc", "hvac", WorkOrderStatus.NEW, WorkOrderPriority.HIGH, 1L, 10L, null, DISPATCHER_EMAIL);

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
        assertEquals("WO-000007", response.getContent().get(0).getWorkOrderNumber());
    }

    @Test
    void updateWorkOrder_WhenSiteMismatch_ShouldThrowBadRequest() {
        UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                .title("Updated title")
                .customerId(1L)
                .siteId(11L)
                .priority(WorkOrderPriority.MEDIUM)
                .workType(WorkType.INSPECTION)
                .build();

        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(siteRepository.findByIdWithCustomer(11L)).thenReturn(Optional.of(otherSite));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.updateWorkOrder(7L, request, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void updateWorkOrder_WhenClosed_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.CLOSED);
        UpdateWorkOrderRequest request = UpdateWorkOrderRequest.builder()
                .title("Updated title")
                .customerId(1L)
                .siteId(10L)
                .priority(WorkOrderPriority.MEDIUM)
                .workType(WorkType.INSPECTION)
                .build();

        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.updateWorkOrder(7L, request, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void assignWorkOrder_WithValidTechnician_ShouldAssignAndTransition() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(userRepository.findById(200L)).thenReturn(Optional.of(technician));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(200L).build(), DISPATCHER_EMAIL);

        assertEquals(WorkOrderStatus.ASSIGNED, workOrder.getStatus());
        assertEquals(technician, workOrder.getAssignedTechnician());
        assertEquals("Terry Tech", response.getAssignedTechnicianName());
    }

    @Test
    void assignWorkOrder_WhenAlreadyAssigned_ShouldReassignWithoutInvalidTransition() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setAssignedTechnician(technician);
        User otherTech = User.builder()
                .id(203L)
                .firstName("Pat")
                .lastName("Field")
                .userEmail("pat@keystone.com")
                .role(Role.TECHNICIAN)
                .enabled(true)
                .build();

        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(userRepository.findById(203L)).thenReturn(Optional.of(otherTech));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(203L).build(), DISPATCHER_EMAIL);

        assertEquals(WorkOrderStatus.ASSIGNED, response.getStatus());
        assertEquals(203L, response.getAssignedTechnicianId());
    }

    @Test
    void assignWorkOrder_WhenManagerSelected_ShouldThrowBadRequest() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(userRepository.findById(201L)).thenReturn(Optional.of(manager));

        ApiException exception = assertThrows(ApiException.class,
                () -> workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(201L).build(), DISPATCHER_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("TECHNICIAN"));
    }

    @Test
    void assignWorkOrder_WhenTechnicianInactive_ShouldThrowBadRequest() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(userRepository.findById(202L)).thenReturn(Optional.of(disabledTechnician));

        ApiException exception = assertThrows(ApiException.class,
                () -> workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(202L).build(), DISPATCHER_EMAIL));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getMessage().contains("inactive"));
    }

    @Test
    void assignWorkOrder_WhenTechnicianMissing_ShouldThrowNotFound() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(999L).build(), DISPATCHER_EMAIL));
    }

    @Test
    void assignWorkOrder_WhenInProgress_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(userRepository.findById(200L)).thenReturn(Optional.of(technician));

        ApiException exception = assertThrows(ApiException.class,
                () -> workOrderService.assignWorkOrder(7L, AssignWorkOrderRequest.builder().technicianId(200L).build(), DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void startWorkOrder_ShouldSetInProgressAndActualStart() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setAssignedTechnician(technician);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrderResponse response = workOrderService.startWorkOrder(7L, DISPATCHER_EMAIL);

        assertEquals(WorkOrderStatus.IN_PROGRESS, response.getStatus());
        assertNotNull(workOrder.getActualStart());
    }

    @Test
    void startWorkOrder_WhenAlreadyStarted_ShouldKeepOriginalActualStart() {
        LocalDateTime originalStart = LocalDateTime.of(2026, 10, 1, 8, 0);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        workOrder.setAssignedTechnician(technician);
        workOrder.setActualStart(originalStart);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        workOrderService.startWorkOrder(7L, DISPATCHER_EMAIL);

        assertEquals(originalStart, workOrder.getActualStart());
    }

    @Test
    void holdResumeCompleteClose_ShouldFollowLifecycle() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        workOrder.setAssignedTechnician(technician);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(WorkOrderStatus.ON_HOLD, workOrderService.holdWorkOrder(7L, DISPATCHER_EMAIL).getStatus());
        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrderService.resumeWorkOrder(7L, DISPATCHER_EMAIL).getStatus());
        assertEquals(WorkOrderStatus.COMPLETED, workOrderService.completeWorkOrder(7L, DISPATCHER_EMAIL).getStatus());
        assertNotNull(workOrder.getActualEnd());
        assertEquals(WorkOrderStatus.CLOSED, workOrderService.closeWorkOrder(7L, DISPATCHER_EMAIL).getStatus());
    }

    @Test
    void completeWorkOrder_WhenAssigned_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.completeWorkOrder(7L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getMessage().contains("ASSIGNED to COMPLETED"));
    }

    @Test
    void closeWorkOrder_WhenInProgress_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.closeWorkOrder(7L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void cancelWorkOrder_FromNew_ShouldSucceed() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(WorkOrderStatus.CANCELLED, workOrderService.cancelWorkOrder(7L, DISPATCHER_EMAIL).getStatus());
    }

    @Test
    void cancelWorkOrder_WhenCompleted_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.cancelWorkOrder(7L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void closedAndCancelled_ShouldRejectFurtherTransitions() {
        workOrder.setStatus(WorkOrderStatus.CLOSED);
        assertThrows(ApiException.class, () -> workOrderService.assertValidTransition(WorkOrderStatus.CLOSED, WorkOrderStatus.IN_PROGRESS));
        assertThrows(ApiException.class, () -> workOrderService.assertValidTransition(WorkOrderStatus.COMPLETED, WorkOrderStatus.NEW));
        assertThrows(ApiException.class, () -> workOrderService.assertValidTransition(WorkOrderStatus.CANCELLED, WorkOrderStatus.ASSIGNED));
    }

    @Test
    void deleteWorkOrder_WhenNew_ShouldDelete() {
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        workOrderService.deleteWorkOrder(7L, DISPATCHER_EMAIL);

        verify(workOrderRepository).delete(workOrder);
    }

    @Test
    void deleteWorkOrder_WhenCompleted_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.deleteWorkOrder(7L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(workOrderRepository, never()).delete(any());
    }

    @Test
    void deleteWorkOrder_WhenClosed_ShouldThrowConflict() {
        workOrder.setStatus(WorkOrderStatus.CLOSED);
        when(workOrderRepository.findByIdWithRelations(7L)).thenReturn(Optional.of(workOrder));

        ApiException exception = assertThrows(ApiException.class, () -> workOrderService.deleteWorkOrder(7L, DISPATCHER_EMAIL));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(workOrderRepository, never()).delete(any());
    }

    private WorkOrder baseWorkOrder(WorkOrderStatus status) {
        return WorkOrder.builder()
                .id(7L)
                .workOrderNumber("WO-000007")
                .title("Replace HVAC filter")
                .description("Quarterly filter replacement")
                .customer(customer)
                .site(site)
                .status(status)
                .priority(WorkOrderPriority.HIGH)
                .workType(WorkType.PREVENTIVE_MAINTENANCE)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CreateWorkOrderRequest validCreateRequest() {
        return CreateWorkOrderRequest.builder()
                .title("Replace HVAC filter")
                .description("Quarterly filter replacement")
                .customerId(1L)
                .siteId(10L)
                .priority(WorkOrderPriority.HIGH)
                .workType(WorkType.PREVENTIVE_MAINTENANCE)
                .scheduledStart(LocalDateTime.of(2026, 10, 4, 9, 0))
                .scheduledEnd(LocalDateTime.of(2026, 10, 4, 12, 0))
                .notes("Bring extra filters")
                .build();
    }
}
